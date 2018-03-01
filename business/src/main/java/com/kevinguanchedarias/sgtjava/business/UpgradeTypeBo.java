package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.sgtjava.entity.UpgradeType;
import com.kevinguanchedarias.sgtjava.repository.UpgradeTypeRepository;

@Service
public class UpgradeTypeBo implements WithNameBo<UpgradeType> {
	private static final long serialVersionUID = 84836919835815466L;

	@Autowired
	private UpgradeTypeRepository upgradeTypeRepository;

	@Override
	public JpaRepository<UpgradeType, Number> getRepository() {
		return upgradeTypeRepository;
	}

	@Override
	public UpgradeType findById(Number id) {
		return upgradeTypeRepository.findOne(id);
	}

}