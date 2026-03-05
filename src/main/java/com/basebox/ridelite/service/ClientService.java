package com.basebox.ridelite.service;

import com.basebox.ridelite.domain.model.Client;
import com.basebox.ridelite.exception.ResourceNotFoundException;
import com.basebox.ridelite.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for Client management.
 * 
 * Simpler than DriverService - clients don't have availability concerns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {
    
    private final ClientRepository clientRepository;
    
    /**
     * Get client by ID.
     * 
     * @param clientId Client ID
     * @return Client entity
     * @throws ResourceNotFoundException if client not found
     */
    @Transactional(readOnly = true)
    public Client getClientById(UUID clientId) {
        log.debug("Fetching client by ID: {}", clientId);
        
        return clientRepository.findById(clientId)
            .orElseThrow(() -> {
                log.error("Client not found with ID: {}", clientId);
                return new ResourceNotFoundException("Client", clientId.toString());
            });
    }
    
    /**
     * Get client by user ID.
     * 
     * USE CASE: User logs in, retrieve their client profile
     * 
     * @param userId User ID
     * @return Client entity
     * @throws ResourceNotFoundException if client profile not found
     */
    @Transactional(readOnly = true)
    public Client getClientByUserId(UUID userId) {
        log.debug("Fetching client by user ID: {}", userId);
        
        return clientRepository.findByUserId(userId)
            .orElseThrow(() -> {
                log.error("Client profile not found for user ID: {}", userId);
                return new ResourceNotFoundException("Client profile", userId.toString());
            });
    }
    
    /**
     * Get client with user details.
     * 
     * @param clientId Client ID
     * @return Client with user loaded
     */
    @Transactional(readOnly = true)
    public Client getClientWithUser(UUID clientId) {
        log.debug("Fetching client with user details: {}", clientId);
        
        return clientRepository.findByIdWithUser(clientId)
            .orElseThrow(() -> new ResourceNotFoundException("Client", clientId.toString()));
    }
    
    /**
     * Get total client count.
     * 
     * @return Total number of clients
     */
    @Transactional(readOnly = true)
    public long getTotalClientCount() {
        return clientRepository.countTotalClients();
    }
}