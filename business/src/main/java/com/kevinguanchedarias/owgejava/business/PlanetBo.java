package com.kevinguanchedarias.owgejava.business;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import com.kevinguanchedarias.owgejava.entity.ExploredPlanet;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUniverseIsFull;
import com.kevinguanchedarias.owgejava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;

@Component
public class PlanetBo implements WithNameBo<Planet> {
	private static final long serialVersionUID = 3000986169771610777L;

	@Autowired
	private PlanetRepository planetRepository;

	@Autowired
	private ExploredPlanetRepository exploredPlanetRepository;

	@Autowired
	private GalaxyBo galaxyBo;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	@Autowired
	private MissionBo missionBo;

	@PersistenceContext
	private transient EntityManager entityManager;

	@Override
	public JpaRepository<Planet, Number> getRepository() {
		return planetRepository;
	}

	@Override
	public EntityManager getEntityManager() {
		return entityManager;
	}

	/**
	 * @param galaxyId
	 *            if null will be a random galaxy
	 * @return Random planet fom galaxy id
	 * @throws SgtBackendUniverseIsFull
	 * @author Kevin Guanche Darias
	 */
	public Planet findRandomPlanet(Integer galaxyId) {
		Integer targetGalaxy = galaxyId;
		if (targetGalaxy == null) {
			targetGalaxy = galaxyBo.findRandomGalaxy();
		}

		planetRepository.findAll();
		int count = (int) (planetRepository.countByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(targetGalaxy));

		if (count == 0) {
			throw new SgtBackendUniverseIsFull("No hay más espacio en este universo");
		}

		int planetLocation = RandomUtils.nextInt(0, count);

		List<Planet> selectedPlanetsRange = planetRepository.findOneByGalaxyIdAndOwnerIsNullAndSpecialLocationIsNull(
				targetGalaxy, new PageRequest(planetLocation, 1));

		return selectedPlanetsRange.get(0);
	}

	/**
	 * 
	 * @param user
	 *            the owner of the planets
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<Planet> findPlanetsByUser(UserStorage user) {
		return planetRepository.findByOwnerId(user.getId());
	}

	/**
	 * Find the planets for logged in user
	 * 
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<Planet> findMyPlanets() {
		return findPlanetsByUser(userStorageBo.findLoggedIn());
	}

	public List<Planet> findByGalaxyAndSectorAndQuadrant(Integer galaxy, Long sector, Long quadrant) {
		return planetRepository.findByGalaxyIdAndSectorAndQuadrant(galaxy, sector, quadrant);
	}

	public boolean isOfUserProperty(UserStorage expectedOwner, Planet planet) {
		return isOfUserProperty(expectedOwner.getId(), planet.getId());
	}

	public boolean isOfUserProperty(Integer expectedOwnerId, Long planetId) {
		return planetRepository.findOneByIdAndOwnerId(planetId, expectedOwnerId) != null;
	}

	public boolean myIsOfUserProperty(Planet planet) {
		return myIsExplored(planet.getId());
	}

	public boolean isHomePlanet(Planet planet) {
		checkPersisted(planet);
		return planet.getHome() != null && planet.getHome();
	}

	public boolean isHomePlanet(Long planetId) {
		return planetRepository.findOneByIdAndHomeTrue(planetId) != null;
	}

	public boolean myIsOfUserProperty(Long planetId) {
		return isOfUserProperty(userStorageBo.findLoggedIn().getId(), planetId);
	}

	public void checkIsOfUserProperty(UserStorage user, Long planetId) {
		if (!isOfUserProperty(user.getId(), planetId)) {
			throw new SgtBackendInvalidInputException(
					"Specified planet with id " + planetId + " does NOT belong to the user");
		}
	}

	public void myCheckIsOfUserProperty(Long planetId) {
		checkIsOfUserProperty(userStorageBo.findLoggedIn(), planetId);
	}

	public boolean isExplored(UserStorage user, Planet planet) {
		return isExplored(user.getId(), planet.getId());
	}

	public boolean isExplored(Integer userId, Long planetId) {
		return isOfUserProperty(userId, planetId)
				|| exploredPlanetRepository.findOneByUserIdAndPlanetId(userId, planetId) != null;
	}

	public boolean myIsExplored(Planet planet) {
		return myIsExplored(planet.getId());
	}

	public boolean myIsExplored(Long planetId) {
		return isExplored(userStorageBo.findLoggedIn().getId(), planetId);
	}

	public void defineAsExplored(UserStorage user, Planet targetPlanet) {
		ExploredPlanet exploredPlanet = new ExploredPlanet();
		exploredPlanet.setUser(user);
		exploredPlanet.setPlanet(targetPlanet);
		exploredPlanetRepository.save(exploredPlanet);
	}

	public void myDefineAsExplored(Planet targetPlanet) {
		defineAsExplored(userStorageBo.findLoggedIn(), targetPlanet);
	}

	/**
	 * Checks if the user, has already the max planets he/she can have
	 * 
	 * @param user
	 * @return True, if the user has already the max planets he/she can have
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean hasMaxPlanets(UserStorage user) {
		checkPersisted(user);
		int factionMax = user.getFaction().getMaxPlanets();
		int userPlanets = planetRepository.countByOwnerId(user.getId());
		return userPlanets >= factionMax;
	}

	public boolean hasMaxPlanets(Integer userId) {
		return hasMaxPlanets(userStorageBo.findById(userId));
	}

	public void doLeavePlanet(Integer invokerId, Long planetId) {
		if (!canLeavePlanet(invokerId, planetId)) {
			throw new SgtBackendInvalidInputException(
					"Can't leave planet, make sure, it is NOT your home planet and you don't have runnings missions, nor running unit constructions");
		}
		Planet planet = findById(planetId);
		planet.setOwner(null);
		save(planet);
	}

	public boolean canLeavePlanet(UserStorage invoker, Planet planet) {
		return canLeavePlanet(invoker.getId(), planet.getId());
	}

	public boolean canLeavePlanet(Integer invokerId, Long planetId) {
		return !isHomePlanet(planetId) && isOfUserProperty(invokerId, planetId)
				&& !obtainedUnitBo.hasUnitsInPlanet(invokerId, planetId)
				&& missionBo.findByUserIdAndTypeCode(invokerId, MissionType.BUILD_UNIT) == null
				&& !missionBo.existsByTargetPlanetAndType(planetId, MissionType.RETURN_MISSION);
	}
}
