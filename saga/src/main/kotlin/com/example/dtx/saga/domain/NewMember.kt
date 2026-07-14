package com.example.dtx.saga.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "member")
class NewMember(
    @Id @GeneratedValue
    var id: Long? = null,
    var email: String,
    var name: String,
)
