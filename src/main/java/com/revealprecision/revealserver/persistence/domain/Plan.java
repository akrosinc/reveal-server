package com.revealprecision.revealserver.persistence.domain;

import com.revealprecision.revealserver.persistence.domain.AbstractAuditableEntity;

import org.hibernate.envers.Audited;

import javax.persistence.*;

@Entity
@Table(name = "plan")
@Audited
public class Plan extends AbstractAuditableEntity {
    //JA DON'T KNOW HOW TO MAP JSON
}