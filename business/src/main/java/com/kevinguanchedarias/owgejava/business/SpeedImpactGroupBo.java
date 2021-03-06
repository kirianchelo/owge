package com.kevinguanchedarias.owgejava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;

@Service
public class SpeedImpactGroupBo implements BaseBo<SpeedImpactGroup> {
	private static final long serialVersionUID = 1367954885113224567L;

	@Autowired
	private SpeedImpactGroupRepository speedImpactGroupRepository;

	@Override
	public JpaRepository<SpeedImpactGroup, Number> getRepository() {
		return speedImpactGroupRepository;
	}

}
