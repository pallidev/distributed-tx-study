package com.example.dtx.saga.repository.legacy

import com.example.dtx.saga.domain.legacy.LegacyMember
import org.springframework.data.jpa.repository.JpaRepository

interface LegacyMemberRepository : JpaRepository<LegacyMember, Long>
