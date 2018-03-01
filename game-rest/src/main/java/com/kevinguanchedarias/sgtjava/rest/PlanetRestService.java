package com.kevinguanchedarias.sgtjava.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.ApplicationScope;

import com.kevinguanchedarias.sgtjava.business.PlanetBo;
import com.kevinguanchedarias.sgtjava.dto.PlanetDto;

@RestController
@RequestMapping("planet")
@ApplicationScope
public class PlanetRestService {

	@Autowired
	private PlanetBo planetBo;

	@RequestMapping(value = "findMyPlanets", method = RequestMethod.GET)
	public Object findMyPlanets() {
		PlanetDto planetDto = new PlanetDto();
		return planetDto.dtoFromEntity(PlanetDto.class, planetBo.findMyPlanets());
	}
}
