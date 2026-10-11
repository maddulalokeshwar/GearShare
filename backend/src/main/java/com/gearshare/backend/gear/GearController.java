
package com.gearshare.backend.gear;

import com.gearshare.backend.user.User;
import com.gearshare.backend.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/gears")
public class GearController {

    private final GearRepository gearRepository;
    private final UserRepository userRepository;

    public GearController(
            GearRepository gearRepository,
            UserRepository userRepository) {
        this.gearRepository = gearRepository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first"
            );
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User account not found"
                ));
    }

    private void verifyOwnership(Gear gear, User user) {
        if (gear.getOwner() == null
                || gear.getOwner().getId() == null
                || user.getId() == null
                || !gear.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only access gear that you own"
            );
        }
    }

    private void validateGear(Gear gear) {
        if (gear == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Gear details are required"
            );
        }

        if (gear.getName() == null || gear.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Gear name cannot be empty"
            );
        }

        if (gear.getName().trim().length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Gear name cannot exceed 100 characters"
            );
        }

        if (gear.getCategory() == null
                || gear.getCategory().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Gear category cannot be empty"
            );
        }

        if (gear.getCategory().trim().length() > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Category cannot exceed 50 characters"
            );
        }

        if (gear.getDescription() != null
                && gear.getDescription().length() > 1000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Description cannot exceed 1000 characters"
            );
        }

        if (gear.getPricePerDay() == null
                || gear.getPricePerDay().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Daily price must be zero or greater"
            );
        }

        if (gear.getPickupAddress() != null
                && gear.getPickupAddress().trim().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pickup address cannot exceed 500 characters"
            );
        }

        Double latitude = gear.getLatitude();
        Double longitude = gear.getLongitude();

        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Latitude and longitude must be provided together"
            );
        }

        if (latitude != null
                && (latitude < -90 || latitude > 90)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Latitude must be between -90 and 90"
            );
        }

        if (longitude != null
                && (longitude < -180 || longitude > 180)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Longitude must be between -180 and 180"
            );
        }

        gear.setName(gear.getName().trim());
        gear.setCategory(gear.getCategory().trim());

        if (gear.getDescription() != null) {
            gear.setDescription(gear.getDescription().trim());
        }

        if (gear.getPickupAddress() != null) {
            String address = gear.getPickupAddress().trim();
            gear.setPickupAddress(address.isEmpty() ? null : address);
        }
    }

    // Public endpoint: never expose private pickup details.
    @GetMapping
    @Transactional(readOnly = true)
    public List<GearResponse> getAllGears() {
        return gearRepository.findAll()
                .stream()
                .map(GearResponse::from)
                .toList();
    }

    // Public endpoint: returns safe listing details only.
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public GearResponse getGearById(@PathVariable Long id) {
        Gear gear = gearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Gear not found with ID: " + id
                ));

        return GearResponse.from(gear);
    }

    // Owner-only endpoint: retrieve private listing details for editing.
    @GetMapping("/{id}/manage")
    @Transactional(readOnly = true)
    public GearManagementResponse getGearForManagement(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Gear gear = gearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Gear not found with ID: " + id
                ));

        verifyOwnership(gear, user);

        return GearManagementResponse.from(gear);
    }

    // Authenticated users can create listings.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GearResponse createGear(
            @RequestBody Gear gear,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        validateGear(gear);

        // Never trust an owner ID supplied by the frontend.
        gear.setOwner(user);

        Gear savedGear = gearRepository.save(gear);

        return GearResponse.from(savedGear);
    }

    // Only the owner can update a listing.
    @PutMapping("/{id}")
    public GearResponse updateGear(
            @PathVariable Long id,
            @RequestBody Gear updatedGear,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Gear existingGear = gearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Gear not found with ID: " + id
                ));

        verifyOwnership(existingGear, user);
        validateGear(updatedGear);

        existingGear.setName(updatedGear.getName());
        existingGear.setDescription(updatedGear.getDescription());
        existingGear.setCategory(updatedGear.getCategory());
        existingGear.setPricePerDay(updatedGear.getPricePerDay());

        // Preserve existing address if an older frontend omits the field.
        if (updatedGear.getPickupAddress() != null) {
            existingGear.setPickupAddress(
                    updatedGear.getPickupAddress()
            );
        }

        // Coordinates must be supplied together.
        // Preserve existing coordinates if both are omitted.
        if (updatedGear.getLatitude() != null
                && updatedGear.getLongitude() != null) {
            existingGear.setLatitude(updatedGear.getLatitude());
            existingGear.setLongitude(updatedGear.getLongitude());
        }

        // The original owner cannot be changed by the request.
        Gear savedGear = gearRepository.save(existingGear);

        return GearResponse.from(savedGear);
    }

    // Only the owner can delete a listing.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGear(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Gear gear = gearRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Gear not found with ID: " + id
                ));

        verifyOwnership(gear, user);
        gearRepository.delete(gear);
    }
}