import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule } from '@angular/forms';

import { MatNativeDateModule, MatRippleModule } from '@angular/material/core';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatCardModule } from '@angular/material/card';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBarModule } from '@angular/material/snack-bar';

import { ChartsModule } from 'ng2-charts';
import { LockerModule } from 'angular-safeguard';
import { LeafletModule } from '@asymmetrik/ngx-leaflet';

import { DeviceComponent } from './pages/device/device.component';
import { HomeComponent } from './pages/home/home.component';
import { NotfoundComponent } from './pages/notfound/notfound.component';
import { LoginComponent } from './pages/login/login.component';
import { MapComponent } from './pages/map/map.component';
import { UserComponent } from './pages/user/user.component';
import { DisabledButtonComponent } from './components/disabled-button/disabled-button.component';
import { LogoutComponent } from './pages/logout/logout.component';

@NgModule({
	declarations: [
		AppComponent,
		DeviceComponent,
		HomeComponent,
		NotfoundComponent,
		LoginComponent,
		MapComponent,
		UserComponent,
		DisabledButtonComponent,
		LogoutComponent
	],
	imports: [
		BrowserModule,
		AppRoutingModule,
		HttpClientModule,
		BrowserAnimationsModule,
		FormsModule,
		MatSidenavModule,
		MatCardModule,
		MatSlideToggleModule,
		MatTabsModule,
		MatProgressSpinnerModule,
		MatInputModule,
		MatButtonModule,
		MatSortModule,
		MatTableModule,
		MatIconModule,
		MatToolbarModule,
		MatListModule,
		MatMenuModule,
		MatBadgeModule,
		MatDatepickerModule,
		MatNativeDateModule,
		MatRippleModule,
		MatTooltipModule,
		MatSnackBarModule,
		ChartsModule,
		LockerModule,
		LeafletModule
	],
	providers: [],
	bootstrap: [AppComponent]
})
export class AppModule {
	constructor() {
	}
}

export interface DrawerTab {
	name: string;
	routerLink: string;
	color?: string;
}
