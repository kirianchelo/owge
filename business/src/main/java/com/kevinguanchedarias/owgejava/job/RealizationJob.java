package com.kevinguanchedarias.owgejava.job;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.CommonException;

public class RealizationJob extends QuartzJobBean {
	private static final Logger LOG = Logger.getLogger(RealizationJob.class);

	private Long missionId;
	private MissionBo missionBo;
	private UnitMissionBo unitMissionBo;

	public Long getMissionId() {
		return missionId;
	}

	public void setMissionId(Long missionId) {
		this.missionId = missionId;
	}

	/**
	 * Will be called at time of resolving a mission
	 * 
	 * @author Kevin Guanche Darias
	 */
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		injectSpringBeans(context);
		Mission mission = missionBo.findById(missionId);
		MissionType missionType = MissionType.valueOf(mission.getType().getCode());
		if (mission != null && !mission.getResolved()) {
			try {
				LOG.debug("Executing mission id " + mission.getId() + " of type "
						+ MissionType.valueOf(mission.getType().getCode()));
				switch (missionType) {
				case BUILD_UNIT:
					missionBo.processBuildUnit(missionId);
					break;
				case LEVEL_UP:
					missionBo.processLevelUpAnUpgrade(missionId);
					break;
				case EXPLORE:
					unitMissionBo.processExplore(missionId);
					break;
				case RETURN_MISSION:
					unitMissionBo.proccessReturnMission(missionId);
					break;
				case GATHER:
					unitMissionBo.processGather(missionId);
					break;
				case ESTABLISH_BASE:
					unitMissionBo.processEstablishBase(missionId);
					break;
				case ATTACK:
					unitMissionBo.processAttack(missionId);
					break;
				case COUNTERATTACK:
					unitMissionBo.processCounterattack(missionId);
					break;
				case CONQUEST:
					unitMissionBo.processConquest(missionId);
					break;
				case DEPLOY:
					unitMissionBo.proccessDeploy(missionId);
					break;
				default:
					throw new CommonException("Unimplemented mission type " + mission.getType().getCode());
				}
			} catch (Exception e) {
				LOG.error("Unexpected fatal exception when executing mission " + missionId, e);
				missionBo.retryMissionIfPossible(missionId);
			}
		}
	}

	private void injectSpringBeans(JobExecutionContext context) {
		try {
			SchedulerContext schedulercontext = context.getScheduler().getContext();
			ApplicationContext applicationContext = (ApplicationContext) schedulercontext.get("applicationContext");
			missionBo = applicationContext.getBean(MissionBo.class);
			unitMissionBo = applicationContext.getBean(UnitMissionBo.class);
		} catch (SchedulerException e) {
			LOG.error("Unexpected exception", e);
			throw new CommonException("Could not get application context inside job parser", e);
		}
	}

}
