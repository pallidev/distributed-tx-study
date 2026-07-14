package com.example.dtx.saga.repository.profile

import com.example.dtx.saga.domain.profile.MemberProfile
import org.springframework.data.jpa.repository.JpaRepository

interface MemberProfileRepository : JpaRepository<MemberProfile, Long>
