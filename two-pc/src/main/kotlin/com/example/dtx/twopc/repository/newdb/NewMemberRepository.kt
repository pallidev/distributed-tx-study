package com.example.dtx.twopc.repository.newdb

import com.example.dtx.twopc.domain.newdb.NewMember
import org.springframework.data.jpa.repository.JpaRepository

interface NewMemberRepository : JpaRepository<NewMember, Long>
