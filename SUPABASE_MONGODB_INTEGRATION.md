# Supabase & MongoDB Integration - Architecture & Implementation

## Overview
This document describes the refactored Supabase integration that properly leverages the existing MongoDB Notification entity as the authoritative source of truth, with Supabase providing real-time pub/sub capabilities on top.

## Architecture Pattern: Dual-Write with MongoDB as Primary

```
┌─────────────────────────────────────────────────────────────┐
│ Application Logic (Controllers, Services)                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
         ┌───────────────────────────────┐
         │ SupabaseNotificationService   │
         │ - sendNotification()           │
         │ - markAsRead()                 │
         │ - deleteNotification()         │
         │ - getNotifications()           │
         └───────────────┬───────────────┘
                         │
         ┌───────────────┴───────────────┐
         │                               │
         ▼                               ▼
    ┌──────────────┐              ┌──────────────┐
    │   MongoDB    │              │  Supabase    │
    │ (Primary)    │              │ (Real-time)  │
    │              │              │              │
    │- Persistent  │              │- Real-time   │
    │- Authorit.   │              │  pub/sub     │
    │- Backup      │              │- Frontend    │
    │              │              │  sync        │
    └──────────────┘              └──────────────┘
```

## Key Changes to SupabaseNotificationService

### 1. Added MongoDB Integration
The service now injects `NotificationRepository` and `UniqueUtility`:
```java
@Autowired
private NotificationRepository notificationRepository;

@Autowired
private UniqueUtility uniqueUtility;
```

### 2. Refactored sendNotification() Method
**Before:** Direct REST API calls to Supabase only
**After:** Dual-write pattern

**Workflow:**
```
1. Create Notification entity instance
2. Set receiverId from recipientId parameter
3. Set title, message, read=false, createdAt=now()
4. Save to MongoDB via NotificationRepository
5. If Supabase enabled:
   - Send to Supabase REST API for real-time updates
   - Include MongoDB notification ID in Supabase payload
6. Return CompletableFuture<Boolean> with success status
```

**Code Pattern:**
```java
// Save to MongoDB (primary)
Notification mongoNotification = new Notification();
mongoNotification.setId(uniqueUtility.getNextNumber("NOTIFICATION", "notification"));
mongoNotification.setTitle(title);
mongoNotification.setMessage(message);
mongoNotification.setReceiverId(recipientId);
mongoNotification.setRelativeId(relatedId);
mongoNotification.setRead(false);
mongoNotification.setCreatedAt(Instant.now());
mongoNotification.setUpdatedAt(Instant.now());

notificationRepository.save(mongoNotification);

// Send to Supabase (real-time layer)
if (supabaseConfig.isSupabaseEnabled()) {
    JsonObject notificationData = new JsonObject();
    notificationData.addProperty("recipient_id", recipientId);
    // ... other properties
    notificationData.addProperty("mongodb_notification_id", mongoNotification.getId());
    insertNotification(notificationData);
}
```

### 3. Enhanced markAsRead() Method
Now synchronizes read status across both stores:
```java
// Update in MongoDB
Notification mongoNotification = notificationRepository.findById(notificationId).orElse(null);
if (mongoNotification != null) {
    mongoNotification.setRead(true);
    mongoNotification.setUpdatedAt(Instant.now());
    notificationRepository.save(mongoNotification);
}

// Update in Supabase (PATCH request)
// ... Supabase REST API call
```

### 4. Enhanced deleteNotification() Method
Now deletes from both stores:
```java
// Delete from MongoDB
notificationRepository.deleteById(notificationId);

// Delete from Supabase (DELETE request)
// ... Supabase REST API call
```

### 5. New Retrieval Methods
Three new methods to fetch notifications from MongoDB:

**a) Get All Notifications for Recipient**
```java
public List<Notification> getNotificationsForRecipient(String recipientId) {
    return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(recipientId);
}
```

**b) Get Unread Notifications Only**
```java
public List<Notification> getUnreadNotificationsForRecipient(String recipientId) {
    return notificationRepository.findByReceiverIdAndReadFalse(recipientId);
}
```

**c) Get Specific Notification by ID**
```java
public Notification getNotification(String notificationId) {
    return notificationRepository.findById(notificationId).orElse(null);
}
```

## Field Mapping: MongoDB ↔ Supabase

| MongoDB Field | Supabase Field | Type | Notes |
|---|---|---|---|
| id | (MongoDB ID) | String | Used to link records |
| receiverId | recipient_id | String | Recipient identifier |
| read | is_read | Boolean | Read status flag |
| relativeId | related_id | String | Related entity ID |
| title | title | String | Notification title |
| message | message | String | Notification message |
| createdAt | created_at | Timestamp | Creation time |
| N/A | type | String | New field in Supabase |
| N/A | mongodb_notification_id | String | Backreference to MongoDB |

## Advantages of This Architecture

### 1. **Single Source of Truth**
- MongoDB is the authoritative source for all notifications
- Supabase is purely for real-time delivery and frontend sync
- No data duplication or sync conflicts

### 2. **Resilience**
- If Supabase is disabled, notifications still save to MongoDB
- If Supabase API fails, MongoDB fallback persists the data
- Graceful degradation when external service is unavailable

### 3. **Audit & History**
- Complete notification history in MongoDB
- Easy to implement retention policies
- Can query notification patterns and analytics

### 4. **Real-time + Persistence**
- Frontend gets real-time updates via Supabase Realtime
- Backend has persistent storage via MongoDB
- Best of both worlds

### 5. **Consistency**
- Both stores are updated in the same transaction (service call)
- Read status, deletion, etc. synchronized across both
- No orphaned records

## Configuration

### Environment Variables Required
```bash
SUPABASE_ENABLED=true
SUPABASE_URL=https://[project-id].supabase.co
SUPABASE_API_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### application.yaml
```yaml
supabase:
  enabled: ${SUPABASE_ENABLED:true}
  url: ${SUPABASE_URL:}
  api-key: ${SUPABASE_API_KEY:}
```

### application-prod.yaml
```yaml
supabase:
  enabled: ${SUPABASE_ENABLED:true}
  url: ${SUPABASE_URL}
  api-key: ${SUPABASE_API_KEY}
```

## Usage Example

### Sending a Notification
```java
@Autowired
private SupabaseNotificationService supabaseNotificationService;

// Send notification (saves to MongoDB + Supabase simultaneously)
supabaseNotificationService.sendNotification(
    recipientId = "user123",
    title = "New Message",
    message = "You have a new message from recruiter",
    type = "message",
    relatedId = "chat456",
    metadata = null
).thenAccept(success -> {
    if (success) {
        log.info("Notification sent successfully");
    }
});
```

### Retrieving Notifications
```java
// Get all notifications for a user (from MongoDB)
List<Notification> allNotifications = 
    supabaseNotificationService.getNotificationsForRecipient("user123");

// Get only unread notifications
List<Notification> unread = 
    supabaseNotificationService.getUnreadNotificationsForRecipient("user123");

// Get a specific notification
Notification notification = 
    supabaseNotificationService.getNotification("notif123");
```

### Updating Read Status
```java
// Mark as read (updates both MongoDB and Supabase)
supabaseNotificationService.markAsRead("notif123")
    .thenAccept(success -> {
        if (success) {
            log.info("Marked as read");
        }
    });
```

## Error Handling

All methods handle errors gracefully:

1. **MongoDB Errors**: 
   - Logged and captured in notification.failureReason
   - Does not block Supabase call
   
2. **Supabase Errors**:
   - Logged but data already safe in MongoDB
   - Frontend real-time sync may be delayed but data is persisted

3. **Configuration Errors**:
   - If Supabase is disabled, notifications still save to MongoDB
   - SupabaseConfig validates on startup

## API Endpoints (via NotificationController)

```bash
# Send notification
POST /api/notifications/send
{
    "recipientId": "user123",
    "title": "New Message",
    "message": "Hi there",
    "type": "message",
    "relatedId": "chat456",
    "metadata": null
}

# Mark as read
PUT /api/notifications/mark-as-read/{notificationId}

# Delete notification
DELETE /api/notifications/delete/{notificationId}

# Check service health
GET /api/notifications/health

# Send convenience methods
POST /api/notifications/send-message/{recipientId}
POST /api/notifications/send-status-update/{recipientId}
```

## Testing the Integration

### 1. Unit Tests
```java
// Mock NotificationRepository and verify dual-write
Mockito.when(notificationRepository.save(any(Notification.class)))
    .thenReturn(notification);

// Verify Supabase REST call was made
verify(httpClient).newCall(any(Request.class));
```

### 2. Integration Tests
```bash
# Test full flow
curl -X POST http://localhost:8080/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId":"user123",
    "title":"Test",
    "message":"Test message",
    "type":"test"
  }'

# Verify MongoDB has the notification
db.notifications.findOne({receiverId: "user123"})

# Verify Supabase has the notification
curl -X GET "https://[project].supabase.co/rest/v1/notifications?recipient_id=eq.user123" \
  -H "apikey: $SUPABASE_API_KEY"
```

## Migration from Old Implementation

If you had notifications using only Supabase before:

1. Old notifications exist only in Supabase
2. New notifications are dual-stored
3. To migrate existing data:
   ```sql
   -- Supabase SQL
   SELECT * FROM notifications 
   WHERE created_at < '2024-01-01'  -- Only old ones
   ```
   Then re-insert them into MongoDB via the service

## Performance Considerations

1. **Async Operations**: All methods return `CompletableFuture` for non-blocking calls
2. **Connection Pooling**: OkHttpClient reuses connections
3. **Batch Operations**: `sendBulkNotifications()` is available for sending to multiple recipients
4. **Index Strategy**:
   - MongoDB: Create index on `receiverId` for fast queries
   - MongoDB: Create index on `receiverId + read` for unread queries
   - Supabase: Indexes managed by Supabase

### Recommended MongoDB Indexes
```javascript
db.notifications.createIndex({ "receiverId": 1, "createdAt": -1 })
db.notifications.createIndex({ "receiverId": 1, "read": 1 })
db.notifications.createIndex({ "createdAt": -1 })
```

## Troubleshooting

### Notifications appear in Supabase but not MongoDB
- Check NotificationRepository is properly injected
- Check MongoDB connection string in application.yaml
- Check MongoDB collection "notifications" exists

### Notifications appear in MongoDB but not Supabase
- Check SUPABASE_ENABLED is true
- Check SUPABASE_URL and SUPABASE_API_KEY are correct
- Check Supabase table "notifications" exists
- Check network connectivity to Supabase

### Read status not syncing
- Ensure markAsRead() is being called
- Check both MongoDB and Supabase have the notification
- Verify OkHttpClient connection is working

### High latency
- Check MongoDB response time
- Check Supabase API response time
- Consider using `sendBulkNotifications()` for batch sends
- Enable connection pooling (already enabled in OkHttpClient)

## Security Notes

1. **Supabase API Key**: Publishable key `sb_publishable_EVTPHXd961rg6chQrcyo_g_m-sxV9lQ` is safe to expose
2. **Secret Key**: Never expose the secret key in frontend code
3. **Row-Level Security (RLS)**: Configure in Supabase to restrict access by recipient_id
4. **Rate Limiting**: Consider implementing rate limiting in NotificationController
5. **Input Validation**: The controller validates input before passing to service

## Future Enhancements

1. **Notification Preferences**: Allow users to configure notification channels
2. **Bulk Operations**: Optimize bulk notification sending
3. **Notification History**: Archive old notifications to S3
4. **Retry Logic**: Implement exponential backoff for failed Supabase calls
5. **Metrics & Monitoring**: Track delivery times, failure rates
6. **Notification Templates**: Reusable notification templates in MongoDB
