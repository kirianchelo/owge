import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';

import { AllianceDetailsComponent } from './components/alliance-details/alliance-details.component';
import { AllianceOfUserComponent } from './components/alliance-of-user/alliance-of-user.component';
import { CoreModule } from '../core/core.module';
import { UserModule } from '../user/user.module';
import { AllianceStorage } from './storages/alliance.storage';
import { RouterModule } from '@angular/router';
import { AllianceService } from './services/alliance.service';
import { AllianceDisplayListComponent } from './components/alliance-display-list/alliance-display-list.component';
import { FormsModule } from '@angular/forms';
import { WidgetsModule } from '../widgets/widgets.module';
import { ListJoinRequestComponent } from './components/list-join-request/list-join-request.component';

/**
 * Module containing all alliance related operations
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.7.0
 * @export
 * @class AllianceModule
 */
@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    CoreModule,
    UserModule,
    RouterModule,
    WidgetsModule,
    TranslateModule.forChild()
  ],
  declarations: [
    AllianceDetailsComponent,
    AllianceOfUserComponent,
    AllianceDisplayListComponent,
    ListJoinRequestComponent
  ]
})
export class AllianceModule {
  public static forRoot(): ModuleWithProviders {
    return {
      ngModule: AllianceModule,
      providers: [AllianceStorage, AllianceService]
    };
  }

  public constructor(private _: AllianceService) {

  }
}
