package com.service.contactUs.repository;

import com.service.contactUs.entity.ContactUsMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IContactUsRepository extends JpaRepository<ContactUsMessage, UUID> {
}
