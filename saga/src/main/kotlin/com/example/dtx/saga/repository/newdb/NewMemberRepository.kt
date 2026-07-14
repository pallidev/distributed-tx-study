package com.example.dtx.saga.repository.newdb

import com.example.dtx.saga.domain.newdb.NewMember
import org.springframework.data.jpa.repository.JpaRepository

interface NewMemberRepository : JpaRepository<NewMember, Long>
