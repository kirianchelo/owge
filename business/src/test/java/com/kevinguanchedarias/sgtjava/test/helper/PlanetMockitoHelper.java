package com.kevinguanchedarias.sgtjava.test.helper;

import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.kevinguanchedarias.sgtjava.business.PlanetBo;
import com.kevinguanchedarias.sgtjava.entity.Planet;

/**
 * This class contains methods useful to fake Planet operations
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class PlanetMockitoHelper {

	private PlanetBo planetBoMock;

	public PlanetMockitoHelper(Object target) {
		planetBoMock = Mockito.mock(PlanetBo.class);
		Whitebox.setInternalState(target, "planetBo", planetBoMock);
	}

	public void fakePlanetExists(Long id, Planet planet) {
		Mockito.when(planetBoMock.findById(id)).thenReturn(planet);
		Mockito.when(planetBoMock.exists(id)).thenReturn(true);
	}
}
