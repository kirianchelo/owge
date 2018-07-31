package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;

public class UserStorageDto implements DtoFromEntity<UserStorage> {

	private Integer id;
	private String username;
	private String email;
	private Double primaryResource;
	private Double secondaryResource;
	private Double consumedEnergy;
	private Double primaryResourceGenerationPerSecond;
	private Double secondaryResourceGenerationPerSecond;
	private Double maxEnergy;
	private FactionDto factionDto;
	private PlanetDto homePlanetDto;
	private ImprovementDto improvements;

	private Double computedPrimaryResourceGenerationPerSecond;
	private Double computedSecondaryResourceGenerationPerSecond;
	private Double computedMaxEnergy;

	@Override
	public void dtoFromEntity(UserStorage entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
		maxEnergy = null;
		if (entity.getImprovements() != null) {
			improvements = new ImprovementDto();
			improvements.dtoFromEntity(entity.getImprovements());
		}
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Double getPrimaryResource() {
		return primaryResource;
	}

	public void setPrimaryResource(Double primaryResource) {
		this.primaryResource = primaryResource;
	}

	public Double getSecondaryResource() {
		return secondaryResource;
	}

	public void setSecondaryResource(Double secondaryResource) {
		this.secondaryResource = secondaryResource;
	}

	public Double getConsumedEnergy() {
		return consumedEnergy;
	}

	public void setConsumedEnergy(Double consumedEnergy) {
		this.consumedEnergy = consumedEnergy;
	}

	public Double getPrimaryResourceGenerationPerSecond() {
		return primaryResourceGenerationPerSecond;
	}

	public void setPrimaryResourceGenerationPerSecond(Double primaryResourceGenerationPerSecond) {
		this.primaryResourceGenerationPerSecond = primaryResourceGenerationPerSecond;
	}

	public Double getSecondaryResourceGenerationPerSecond() {
		return secondaryResourceGenerationPerSecond;
	}

	public void setSecondaryResourceGenerationPerSecond(Double secondaryResourceGenerationPerSecond) {
		this.secondaryResourceGenerationPerSecond = secondaryResourceGenerationPerSecond;
	}

	public Double getMaxEnergy() {
		return maxEnergy;
	}

	public void setMaxEnergy(Double maxEnergy) {
		this.maxEnergy = maxEnergy;
	}

	public FactionDto getFactionDto() {
		return factionDto;
	}

	public void setFactionDto(FactionDto factionDto) {
		this.factionDto = factionDto;
	}

	public PlanetDto getHomePlanetDto() {
		return homePlanetDto;
	}

	public void setHomePlanetDto(PlanetDto planetDto) {
		this.homePlanetDto = planetDto;
	}

	public ImprovementDto getImprovements() {
		return improvements;
	}

	public void setImprovements(ImprovementDto improvements) {
		this.improvements = improvements;
	}

	public Double getComputedPrimaryResourceGenerationPerSecond() {
		return computedPrimaryResourceGenerationPerSecond;
	}

	public void setComputedPrimaryResourceGenerationPerSecond(Double computedPrimaryResourceGenerationPerSecond) {
		this.computedPrimaryResourceGenerationPerSecond = computedPrimaryResourceGenerationPerSecond;
	}

	public Double getComputedSecondaryResourceGenerationPerSecond() {
		return computedSecondaryResourceGenerationPerSecond;
	}

	public void setComputedSecondaryResourceGenerationPerSecond(Double computedSecondaryResourceGenerationPerSecond) {
		this.computedSecondaryResourceGenerationPerSecond = computedSecondaryResourceGenerationPerSecond;
	}

	public Double getComputedMaxEnergy() {
		return computedMaxEnergy;
	}

	public void setComputedMaxEnergy(Double computedMaxEnergy) {
		this.computedMaxEnergy = computedMaxEnergy;
	}

}
