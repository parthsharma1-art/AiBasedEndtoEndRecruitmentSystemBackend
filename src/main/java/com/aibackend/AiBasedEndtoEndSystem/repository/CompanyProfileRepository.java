package com.aibackend.AiBasedEndtoEndSystem.repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyProfileRepository extends MongoRepository<CompanyProfile, String> {

    Optional<CompanyProfile> getCompanyProfileByRecruiterId(String recruiterId);


    Optional<CompanyProfile> findByBasicSettingCompanyDomain(String companyDomain);
}
