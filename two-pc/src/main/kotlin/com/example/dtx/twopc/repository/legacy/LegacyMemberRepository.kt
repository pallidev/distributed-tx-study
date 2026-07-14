package com.example.dtx.twopc.repository.legacy

import com.example.dtx.twopc.domain.legacy.LegacyMember
import org.springframework.data.jpa.repository.JpaRepository

interface LegacyMemberRepository : JpaRepository<LegacyMember, Long>
