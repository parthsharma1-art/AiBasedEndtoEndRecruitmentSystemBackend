# Supabase Integration Guide

## Overview

This guide explains how to integrate Supabase for real-time notifications in the AI Based End-to-End Recruitment System.

## Prerequisites

- Supabase Project (free tier available at https://supabase.com)
- Supabase Project URL
- Supabase Publishable API Key (the one you provided)

## Setup Steps

### 1. Supabase Project Setup

1. Go to [Supabase](https://supabase.com) and create a new project
2. Once created, note your:
   - **Project URL** (e.g., `https://your-project.supabase.co`)
   - **Publishable API Key** (already provided: `sb_publishable_EVTPHXd961rg6chQrcyo_g_m-sxV9lQ`)

### 2. Create Notifications Table in Supabase

Execute this SQL in your Supabase SQL Editor:

```sql
-- Create notifications table
CREATE TABLE notifications (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  recipient_id TEXT NOT NULL,
  title TEXT NOT NULL,
  message TEXT NOT NULL,
  type TEXT DEFAULT 'alert',
  related_id TEXT,
  is_read BOOLEAN DEFAULT false,
  metadata JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create index for faster queries
CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);

-- Enable RLS (Row Level Security) - optional but recommended
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Create policy allowing users to read their own notifications
CREATE POLICY "Users can read their own notifications"
  ON notifications FOR SELECT
  USING (recipient_id = auth.uid()::text OR recipient_id = current_user_id());
```

### 3. Environment Configuration

Add the following environment variables to your deployment platform:

**For Development (local):**
- `SUPABASE_URL=` (leave empty initially)
- `SUPABASE_API_KEY=sb_publishable_EVTPHXd961rg6chQrcyo_g_m-sxV9lQ`
- `SUPABASE_ENABLED=false` (set to false for local testing without Supabase)

**For Production (Render/Vercel):**
- `SUPABASE_URL=<your-supabase-url>`
- `SUPABASE_API_KEY=sb_publishable_EVTPHXd961rg6chQrcyo_g_m-sxV9lQ`
- `SUPABASE_ENABLED=true`

### 4. Application Configuration

The following files have been updated:

#### `pom.xml`
- Added Supabase SDK dependency
- Added OkHttp3 for HTTP client
- Added Gson for JSON processing

#### `src/main/resources/application.yaml`
```yaml
supabase:
  enabled: ${SUPABASE_ENABLED:true}
  url: ${SUPABASE_URL:}
  api-key: ${SUPABASE_API_KEY:}
```

#### `src/main/resources/application-prod.yaml`
```yaml
supabase:
  enabled: ${SUPABASE_ENABLED:true}
  url: ${SUPABASE_URL}
  api-key: ${SUPABASE_API_KEY}
```

## Implementation Classes

### 1. **SupabaseConfig.java**
- Manages Supabase configuration
- Location: `src/main/java/com/aibackend/AiBasedEndtoEndSystem/config/SupabaseConfig.java`
- Provides URL and API key access
- Validates configuration on startup

### 2. **SupabaseNotificationService.java**
- Core notification service
- Location: `src/main/java/com/aibackend/AiBasedEndtoEndSystem/service/SupabaseNotificationService.java`
- Async notification operations using CompletableFuture
- Methods:
  - `sendNotification()` - Send generic notification
  - `sendMessageNotification()` - Send message notification
  - `sendStatusUpdateNotification()` - Send status updates
  - `sendBulkNotifications()` - Send to multiple recipients
  - `markAsRead()` - Mark notification as read
  - `deleteNotification()` - Delete notification

### 3. **NotificationController.java**
- REST endpoints for notifications
- Location: `src/main/java/com/aibackend/AiBasedEndtoEndSystem/controller/NotificationController.java`
- Endpoints:
  - `POST /api/notifications/send` - Send notification
  - `POST /api/notifications/send-message/{recipientId}` - Send message
  - `POST /api/notifications/send-status-update/{recipientId}` - Send status update
  - `PUT /api/notifications/mark-as-read/{notificationId}` - Mark as read
  - `DELETE /api/notifications/delete/{notificationId}` - Delete notification
  - `GET /api/notifications/health` - Health check

### 4. **SupabaseNotificationRequest.java**
- DTO for notification requests
- Location: `src/main/java/com/aibackend/AiBasedEndtoEndSystem/dto/SupabaseNotificationRequest.java`

## Usage Examples

### Example 1: Send a Simple Notification

```bash
curl -X POST http://localhost:8081/api/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": "candidate-123",
    "title": "Job Application Status",
    "message": "Your application has been shortlisted!",
    "type": "status_update",
    "relatedId": "job-456"
  }'
```

### Example 2: Send Message Notification

```bash
curl -X POST http://localhost:8081/api/notifications/send-message/candidate-123 \
  -G \
  --data-urlencode "senderId=recruiter-456" \
  --data-urlencode "message=Can you provide more details about your experience?"
```

### Example 3: Send Bulk Notification (Java Code)

```java
@Autowired
private SupabaseNotificationService notificationService;

List<String> recipientIds = Arrays.asList("user1", "user2", "user3");
CompletableFuture<Integer> result = notificationService.sendBulkNotifications(
    recipientIds,
    "New Job Posting",
    "A new job has been posted matching your profile",
    "job_posting",
    "job-789"
);

int successCount = result.join();
System.out.println("Successfully sent to " + successCount + " recipients");
```

### Example 4: Mark Notification as Read

```java
CompletableFuture<Boolean> result = notificationService.markAsRead("notification-id-123");
boolean success = result.join();
```

## Integration with Existing Services

To integrate Supabase notifications with existing services, inject `SupabaseNotificationService`:

```java
@Service
public class YourService {
    
    @Autowired
    private SupabaseNotificationService notificationService;
    
    public void notifyUserOfStatusChange(String userId, String newStatus) {
        notificationService.sendStatusUpdateNotification(
            userId,
            "Status Update",
            "Your application status has changed to: " + newStatus,
            "application-123"
        );
    }
}
```

## Frontend Integration (JavaScript/React)

To listen for real-time notifications on the frontend, use Supabase client:

```javascript
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  'https://your-project.supabase.co',
  'sb_publishable_EVTPHXd961rg6chQrcyo_g_m-sxV9lQ'
);

// Subscribe to real-time notifications for current user
const subscription = supabase
  .from('notifications')
  .on('*', payload => {
    console.log('New notification:', payload.new);
    // Update UI with new notification
  })
  .subscribe();

// Cleanup on component unmount
subscription.unsubscribe();
```

## Troubleshooting

### Issue: "Supabase URL is not configured"
- **Solution**: Set the `SUPABASE_URL` environment variable with your Supabase project URL

### Issue: "Failed to insert notification"
- **Solution**: 
  - Verify the notifications table exists in Supabase
  - Check that the API key is correct
  - Ensure Row Level Security (RLS) policies allow the operation

### Issue: Async timeout
- **Solution**: Increase timeout in the config or use `.join()` with a timeout parameter

### Issue: Connection refused
- **Solution**: 
  - Verify Supabase is running
  - Check network connectivity
  - Ensure firewall allows outbound HTTPS connections

## Security Notes

⚠️ **Important**: The publishable key you provided is safe to expose in client-side code. It's designed for that purpose.

However:
- Never expose your **Secret Key** (different from publishable key)
- Always use HTTPS in production
- Implement proper authentication/authorization
- Use Row Level Security (RLS) in Supabase for data protection

## Performance Considerations

- Notifications are sent asynchronously using CompletableFuture
- Bulk notifications are sent in parallel for better performance
- Consider adding rate limiting for high-volume notifications
- Monitor Supabase API usage to avoid exceeding quotas

## Database Backup

Supabase provides automatic daily backups. You can also:
1. Export your data regularly
2. Monitor database growth
3. Archive old notifications to reduce storage

## Next Steps

1. Set up your Supabase project
2. Create the notifications table using the provided SQL
3. Configure environment variables
4. Test the API endpoints
5. Integrate with your existing services
6. Set up frontend subscription listeners
7. Monitor and optimize as needed

## Support

For issues or questions:
- Supabase Documentation: https://supabase.com/docs
- GitHub Issues: Check the project repository
- Community Forum: Ask on Supabase forum

## References

- [Supabase Documentation](https://supabase.com/docs)
- [Supabase Realtime](https://supabase.com/docs/guides/realtime)
- [Supabase Rest API](https://supabase.com/docs/guides/api)
- [PostgREST Documentation](https://postgrest.org)
