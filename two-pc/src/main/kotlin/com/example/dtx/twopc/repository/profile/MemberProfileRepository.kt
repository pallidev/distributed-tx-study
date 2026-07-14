package com.example.dtx.twopc.repository.profile

import com.example.dtx.twopc.domain.profile.MemberProfile
import org.springframework.data.jpa.repository.JpaRepository

interface MemberProfileRepository : JpaRepository<MemberProfile, Long>
