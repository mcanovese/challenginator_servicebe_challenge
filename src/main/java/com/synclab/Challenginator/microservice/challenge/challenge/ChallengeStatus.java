package com.synclab.Challenginator.microservice.challenge.challenge;

import javax.persistence.Enumerated;


public enum ChallengeStatus {

    PENDING,
    ACCEPTED,
    REFUSED,
    EVALUATING,
    TERMINATED
}
