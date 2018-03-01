import { ObtainedUnit } from '../shared-pojo/obtained-unit.pojo';
import { GameBaseService } from './game-base.service';
import { ResourcesEnum } from '../shared-enum/resources-enum';

import { RequirementPojo } from './../shared-pojo/requirement.pojo';
import { RunningUnitPojo } from './../shared-pojo/running-unit-build.pojo';
import { URLSearchParams } from '@angular/http';
import { PlanetPojo } from './../shared-pojo/planet.pojo';
import { PlanetService } from './planet.service';
import { UnitPojo } from './../shared-pojo/unit.pojo';
import { Observable } from 'rxjs/Rx';
import { ResourceManagerService } from './resource-manager.service';
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import 'rxjs/add/operator/filter';

export class PlanetsNotReadyError extends Error { }

export class RunningUnitIntervalInformation {
  public interval: number;
  public missionData: RunningUnitPojo;

  constructor(interval: number, missionData: RunningUnitPojo) {
    this.interval = interval;
    this.missionData = missionData;
  }
}

@Injectable()
export class UnitService extends GameBaseService {

  private _planetList: PlanetPojo[];

  /** @var {number} provides access to each interval, of running build mission (if any), the key value is like, planetId => intervalId */
  private _intervals: RunningUnitIntervalInformation[] = [];

  /**
   * Some functions MUST only be invoked when the planets has been loaded <br>
   * For example: Asking if there is a running unit recluit mission, can't be done <br />
   * This behavior subject value is true when the list has been loaded
   *
   * @author Kevin Guanche Darias
   */
  public get planetsLoaded(): BehaviorSubject<boolean> {
    return this._planetsLoaded;
  }
  private _planetsLoaded: BehaviorSubject<boolean> = new BehaviorSubject(false);

  constructor(
    private _resourceManagerService: ResourceManagerService, private _planetService: PlanetService
  ) {
    super();
    this.resourcesAutoUpdate();
    this._subscribeToPlanetChanges();
    this.getLoginSessionService().findSelectedPlanet.subscribe(currentSelected => this._selectedPlanet = currentSelected);
  }

  /**
   * Returns the list of unlocked units!
   *
   * @author Kevin Guanche Darias
   */
  public findUnlocked(): Observable<UnitPojo> {
    return this.doGetWithAuthorizationToGame('unit/findUnlocked');
  }

  /**
   * Checks if there is a unit building in the selected planet<br>
   * <b>IMPORTANT:</b> Can only be used after planets has been loaded
   *
   * @author Kevin Guanche Darias
   */
  public findIsRunningInSelectedPlanet(): RunningUnitIntervalInformation {
    return this._findRunningBuildWithData(this._selectedPlanet.id);
  }

  /**
   * Computes required resources by the next upgrade level
   *
   * @param {UnitPojo} unit
   *          - Notice: this function alters this object
   * @param {boolean} subscribeToResources true if want to recompute the runnable field of RequirementPojo,
   *          on each change to the resources (expensive!)
   * @param {BehaviorSubject<number>} countBehabiorSubject - Specify it to automatically update resource requirements on changes to count
   * @returns obtainedUpgrade with filled values
   * @author Kevin Guanche Darias
   */
  public computeRequiredResources(unit: UnitPojo, subscribeToResources: boolean, countBehaviorSubject: BehaviorSubject<number>): UnitPojo {
    if (!countBehaviorSubject) {
      this._doComputeRequiredResources(unit, subscribeToResources);
    } else {
      countBehaviorSubject.subscribe(newCount => {
        if (unit.requirements) {
          unit.requirements.stopDynamicRunnable();
        }
        unit.requirements = new RequirementPojo();
        unit.requirements.requiredPrimary = unit.primaryResource * newCount;
        unit.requirements.requiredSecondary = unit.secondaryResource * newCount;
        unit.requirements.requiredTime = unit.time * newCount;
        this._doCheckResourcesSubscriptionForRequirements(unit.requirements, subscribeToResources);
      });
    }
    return unit;
  }

  /**
   * Will register a unit build mission <b>for selected planet</b>
   * <b>NOTICE:</b> If success auto updates the user resources based on requirements
   *
   * @param {UnitPojo} unit to be build
   * @param {number} count  of that unit
   * @todo Finish it!
   * @author Kevin Guanche Darias
   */
  public registerUnitBuild(unit: UnitPojo, count: number): void {
    let params: URLSearchParams = new URLSearchParams();
    unit = this._doComputeRequiredResources(unit, false, count);
    params.append('planetId', this._selectedPlanet.id.toString());
    params.append('unitId', unit.id.toString());
    params.append('count', count.toString());
    this.doGetWithAuthorizationToGame('unit/build', params).subscribe(res => {
      this._resourceManagerService.minusResources(ResourcesEnum.PRIMARY, unit.requirements.requiredPrimary);
      this._resourceManagerService.minusResources(ResourcesEnum.SECONDARY, unit.requirements.requiredSecondary);
      if (res) {
        res.terminationDate = new Date(res.terminationDate);
        this._registerInterval(this._selectedPlanet, res);
        this._refreshPlanetsLoaded();
      }
    });
  }

  /**
   * Cancels a unit build mission
   * 
   * @todo https://trello.com/c/WPx0qaXR/23-unitservicecancel-should-not-call-thissubscribetoplanetchanges
   * @param {RunningUnitPojo} missionData 
   * @memberof UnitService
   * @author Kevin Guanche Darias
   */
  public cancel(missionData: RunningUnitPojo) {
    let params: URLSearchParams = new URLSearchParams();
    params.append('missionId', missionData.missionId);
    this.doGetWithAuthorizationToGame('unit/cancel', params).subscribe(() => {
      this._resourceManagerService.addResources(ResourcesEnum.PRIMARY, missionData.requiredPrimary);
      this._resourceManagerService.addResources(ResourcesEnum.SECONDARY, missionData.requiredSecondary);
      this._subscribeToPlanetChanges();
    });
  }

  /**
   * Finds unit in selected planet <br>
   * <b>NOTICE:</b> Backend should throw if you do not own the planet
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} planetId
   * @returns {Observable<ObtainedUnit[]>}
   * @memberof UnitService
   */
  public findInMyPlanet(planetId: number): Observable<ObtainedUnit[]> {
    const params: URLSearchParams = new URLSearchParams();
    params.append('planetId', planetId.toString());
    return this.doGetWithAuthorizationToGame('unit/findInMyPlanet', params);
  }

  /**
   * Will listen to planet changes, that the planet service has emmited! <br />
   * And clears and register new intervals (Asking the server if there is a new mission)
   *
   * @author Kevin Guanche Darias
   */
  private _subscribeToPlanetChanges(): void {
    this.planetsLoaded.next(false);
    this._planetService.myPlanets.filter(myPlanets => !!myPlanets).subscribe(myPlanets => {
      this._clearIntervals();
      this._planetList = myPlanets;
      this._registerIntervals(myPlanets);
      this.planetsLoaded.next(true);
    });
  }

  private _clearIntervals(): void {
    this._intervals.forEach(value => window.clearInterval(value.interval));
    this._intervals = [];
  }

  private _registerInterval(planet: PlanetPojo, runningMission: RunningUnitPojo): void {
    this._intervals[planet.id] = new RunningUnitIntervalInformation(
      window.setInterval(() => this._handleMissionTermination(runningMission), 1500),
      runningMission
    );
  }
  private _registerIntervals(planets: PlanetPojo[]): void {
    planets.forEach(currentPlanet => {
      this._findRunningBuild(currentPlanet).filter(runningMission => !!runningMission).subscribe(runningMission => {
        runningMission.terminationDate = new Date(runningMission.terminationDate);
        this._registerInterval(currentPlanet, runningMission);
      });
    });
  }

  /**
   * @todo THIS METHOD IS NOT IMPLEMENTED!!!, DO IT!!!!
   * @author Kevin Guanche Darias
   */
  private _handleMissionTermination(targetPlanet: RunningUnitPojo): void {
    let now: Date = new Date();
    now = new Date(now.getTime() - 1000);
  }

  /**
   * Will ask the server if the selected planet has a unit build mission going!
   *
   * @param {PlanetPojo} planet - Planet to ask for
   * @author Kevin Guanche Darias
   */
  private _findRunningBuild(planet: PlanetPojo): Observable<RunningUnitPojo> {
    let params: URLSearchParams = new URLSearchParams();
    params.append('planetId', planet.id.toString());
    return this.doGetWithAuthorizationToGame('unit/findRunning', params);
  }

  /**
   * From the registered intervals, returns the data
   */
  private _findRunningBuildWithData(planetId: number): RunningUnitIntervalInformation {
    if (!this.planetsLoaded.value) {
      throw new PlanetsNotReadyError('Can\'t invoke this method when planets has not been loaded!');
    }

    for (let currentPlanetId in this._intervals) {
      if (currentPlanetId === planetId.toString()) {
        return this._intervals[currentPlanetId];
      }
    }
    return null;
  }

  private _doComputeRequiredResources(unit: UnitPojo, subscribeToResources: boolean, count = 1): UnitPojo {
    let requirements: RequirementPojo = new RequirementPojo();
    requirements.requiredPrimary = unit.primaryResource * count;
    requirements.requiredSecondary = unit.secondaryResource * count;
    requirements.requiredTime = unit.time * count;

    this._doCheckResourcesSubscriptionForRequirements(requirements, subscribeToResources);
    unit.requirements = requirements;
    return unit;
  }

  private _doCheckResourcesSubscriptionForRequirements(requirements: RequirementPojo, subscribeToResources: boolean) {
    if (subscribeToResources) {
      requirements.startDynamicRunnable(this._resourceManagerService);
    } else {
      requirements.checkRunnable(this.resources);
    }
  }

  /**
   * When a planet change his conditions, for example because it's now recluiting, this will force BehaviorSubject to fire again
   * 
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @private
   * @memberof UnitService
   */
  private _refreshPlanetsLoaded(): void {
    this._planetsLoaded.next(false);
    this._planetsLoaded.next(true);
  }
}
