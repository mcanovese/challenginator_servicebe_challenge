package com.synclab.Challenginator.microservice.challenge.challenge;

import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;


import javax.management.BadAttributeValueExpException;
import java.time.LocalDateTime;
import java.util.List;

/*
service principale delle sfide (non gestisce inserimento che è gestito a parte in un servizio dedicato)

 */

@Service
public class ChallengeService {


    private  ChallengeRepository challengeRepository;


    @Autowired
    private RestTemplate restTemplate;


    public ChallengeService(ChallengeRepository challengeRepository) {
        this.challengeRepository = challengeRepository;
    }

    //ricerco sfide in cui ID è sfidante
    public List<Challenge> getUserChallenge(Long id){

        return challengeRepository.findChallengesByChallenger(id);
    }

    //ricerco sfide in cuiId è sfidato
    public List<Challenge> getUserChallengeOut(Long id){
        return challengeRepository.findChallengesByChallenged(id);
    }

    public List<Challenge> getALlChallenge(Long id){
        return challengeRepository.getAllChallenge(id);
    }

    public Challenge getChallengeById(Long id){
        return challengeRepository.getChallengeById(id);
    }

    public boolean updateChallenge(Challenge challenge){
        Challenge updated =  challengeRepository.save(challenge);
        if(updated.getId() == challenge.getId()) return true;
        else return false;


    }
    // inserisco nuova sfida
    public HttpStatus insertNewChallenge(Challenge challenge){
        challengeRepository.save(challenge);
       return HttpStatus.OK;
    }

    //ottengo challenge da valutare in qualità di valutatore
    public List<Challenge> getChallengeToEvaluate(Long id){
        return challengeRepository.findChallengesByEvaluator(id);
    }

    // valuto challenge
    public Challenge evaluateChallenge(ValutatorRequest request, Long valutator){
        Challenge challengeToEvaluate = getChallengeById(request.getChallengeId());
        //verifica valutatore
        if(challengeToEvaluate.getEvaluator() == valutator){
            challengeToEvaluate.setResult(request.getResult());
            challengeToEvaluate.setStatus(ChallengeStatus.TERMINATED);
            updateChallenge(challengeToEvaluate);
            return challengeToEvaluate;
        } else throw new HttpServerErrorException(HttpStatus.BAD_REQUEST);
    }

    //posso effettare ACCETTAZIONE ARRESA RIFIUTO COMPLETAMENTO di una challenge a seconda di cosa contiene la richiesta
    public Challenge userChallengeOperation(ControlRequest request, Long userId){
        Challenge challengeToOperate = getChallengeById(request.getChallengeId());

        if(challengeToOperate.getChallenged() != userId) throw  new HttpServerErrorException(HttpStatus.BAD_REQUEST);

        if(request.isAccept()){
            challengeToOperate.setTimestamp_acceptance(LocalDateTime.now());
            challengeToOperate.setStatus(ChallengeStatus.ACCEPTED);
            updateChallenge(challengeToOperate);
            return challengeToOperate;
        }

        if(request.isGiveup()){
            challengeToOperate.setResult(ChallengeResult.GIVEUP);
            challengeToOperate.setStatus(ChallengeStatus.TERMINATED);
            updateChallenge(challengeToOperate);
            return challengeToOperate;
        }

        if(request.isRefuse()){
            challengeToOperate.setStatus(ChallengeStatus.REFUSED);
            updateChallenge(challengeToOperate);
            return challengeToOperate;
        }

        if(request.isComplete()){
            challengeToOperate.setStatus(ChallengeStatus.EVALUATING);
            updateChallenge(challengeToOperate);
            return challengeToOperate;
        } else throw  new HttpServerErrorException(HttpStatus.BAD_REQUEST);
    }

    // cancella una challenge
    public HttpStatus deleteChallenge(Long challengeId, Long userId){
        Challenge challengeToDelete = getChallengeById(challengeId);

        if(challengeToDelete.getChallenger() != userId) return  HttpStatus.BAD_REQUEST;
        challengeRepository.deleteById(challengeId);
        return  HttpStatus.OK;
    }

    // verifico autenticazione contattando il micorservizio utenti
    public Long authCheck(String jwt) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", jwt);
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        // Contatto il servizio utenti ed ottengo JSON con dettagli utente, se jwt is valid
        Long response = null;
        try{
            response = restTemplate.exchange("http://localhost:8080/user/authcheck",
                    HttpMethod.POST, entity, Long.class).getBody();
        } catch (Exception e){
            throw new HttpServerErrorException(HttpStatus.BAD_REQUEST);
        }

        if (response == null)  throw new HttpServerErrorException(HttpStatus.BAD_REQUEST);
        else return response;

    }
}
