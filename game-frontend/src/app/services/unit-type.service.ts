import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
const camelCase = require('lodash.camelcase');
const upperFirst = require('lodash.upperfirst');

import { GameBaseService } from '../service/game-base.service';
import { UnitType } from '../shared/types/unit-type.type';
import { ProgrammingError } from '../../error/programming.error';
import { MissionType } from '../shared/types/mission.type';
import { PlanetPojo } from '../shared-pojo/planet.pojo';
import { MissionSupport } from '../shared/types/mission-support.type';

@Injectable()
export class UnitTypeService extends GameBaseService<UnitType> {

  public constructor() {
    super();
    this.loadTypes();
  }

  public getUnitTypes(): Observable<UnitType[]> {
    return this._subjectToObservable();
  }

  public loadTypes(): void {
    this._loadSubject('unitType/', async result => result.map(current => {
      if (!current.userBuilt) {
        current.userBuilt = 0;
      }
      return current;
    }));
  }


  /**
   * Returns the available number that the user can build of a given unit type
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} id unit type id
   * @returns {number}
   * @throws {ProgrammingError} If type doesn't exists
   * @memberof UnitTypeService
   */
  public findAvailableByType(id: number): number {
    const type: UnitType = this._findTypeById(id);
    return type.computedMaxCount - type.userBuilt;
  }


  /**
   * Adds to userbuilt count of the specified type <br>
   * <b>NOTICE:</b> Will trigger a new value to observers
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} id
   * @param {number} count
   * @throws {ProgrammingError} If type doesn't exists
   * @memberof UnitTypeService
   */
  public addToType(id: number, count: number): void {
    const type: UnitType = this._findTypeById(id);
    type.userBuilt += count;
    this._loadableBehaviorSubject.next(this._loadableBehaviorSubject.value);
  }

  /**
   * Sustract to userbuilt count of the specified type <br>
   * <b>NOTICE:</b> Will trigger a new value to observers
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} id
   * @param {number} count
   * @throws {ProgrammingError} If type doesn't exists
   * @memberof UnitTypeService
   */
  public sustractToType(id: number, count: number): void {
    this.addToType(id, -count);
  }


  /**
   * Returns true, if user has enough avaiable for the given unit type
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {number} typeId
   * @param {number} requiredCount Count wanted to be added by the user
   * @returns {boolean}
   * @memberof UnitTypeService
   */
  public hasAvailable(typeId: number, requiredCount: number): boolean {
    const type: UnitType = this._findTypeById(typeId);
    return !type.maxCount || this.findAvailableByType(typeId) >= requiredCount;
  }

  /**
   * Test if all the unitTypes passed can do the specified mission
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {PlanetPojo} planet
   * @param {UnitType[]} unitTypes
   * @param {MissionType} missionType
   * @returns {boolean}
   * @memberof UnitTypeService
   */
  public canDoMission(planet: PlanetPojo, unitTypes: UnitType[], missionType: MissionType): boolean {
    return unitTypes.every(current => {
      const status: MissionSupport = current[`can${upperFirst(camelCase(missionType))}`];
      switch (status) {
        case 'ANY':
          return true;
        case 'NONE':
          return false;
        case 'OWNED_ONLY':
          return planet.ownerId === this._loginSessionService.findTokenData().id;
        default:
          throw new ProgrammingError(`Unsupported MissionSupport ${status}`);
      }
    });
  }


  /**
   * Converts an array of ids to an array of unitTypes
   *
   * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
   * @param {...number[]} ids
   * @returns {Promise<UnitType[]>}
   * @memberof UnitTypeService
   */
  public idsToUnitTypes(...ids: number[]): Promise<UnitType[]> {
    return new Promise(resolve => {
      this.getUnitTypes().subscribe(result => {
        resolve(ids.map(currentId => result.find(currentUnitType => currentUnitType.id === currentId)));
      });
    });
  }

  private _findTypeById(id: number): UnitType {
    const retVal: UnitType = this._loadableBehaviorSubject.value.find(current => current.id === id);
    if (!retVal) {
      throw new ProgrammingError(`No UnitType with id ${id} was found`);
    }
    return retVal;
  }
}
