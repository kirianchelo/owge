package com.kevinguanchedarias.owgejava.business;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.repository.FactionRepository;

@Service
public class FactionBo implements WithNameBo<Faction> {
	private static final long serialVersionUID = -6735454832872729630L;

	@Autowired
	private FactionRepository factionRepository;

	@Override
	public JpaRepository<Faction, Number> getRepository() {
		return factionRepository;
	}

	/**
	 * Returns the factions that are visible
	 * 
	 * @param lazyFetch
	 *            Fetch the proxies, or set to null
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public List<Faction> findVisible(boolean lazyFetch) {
		List<Faction> retVal = factionRepository.findByHiddenFalse();
		handleLazyFetch(lazyFetch, retVal);
		return retVal;
	}

	/**
	 * Will check if given faction exists AND it's visible
	 * 
	 * @param id
	 *            faction id
	 * @return
	 * @author Kevin Guanche Darias
	 */
	public boolean existsAndIsVisible(Integer id) {
		return factionRepository.countByHiddenFalseAndId(id) == 1;
	}

	private void handleLazyFetch(boolean lazyFetch, List<Faction> factions) {
		if (lazyFetch) {
			for (Faction current : factions) {
				current.getImprovement().getId();
			}
		} else {
			for (Faction current : factions) {
				current.setImprovement(null);
			}
		}
	}
}
