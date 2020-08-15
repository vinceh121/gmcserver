import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { DeviceComponent } from './pages/device/device.component';
import { NotfoundComponent } from './pages/notfound/notfound.component';
import { LoginComponent } from './pages/login/login.component';
import { LogoutComponent } from './pages/logout/logout.component';
import { MapComponent } from './pages/map/map.component';
import { UserComponent } from './pages/user/user.component';


const routes: Routes = [
	{ path: '', component: HomeComponent },
	{ path: 'device/:id', component: DeviceComponent },
	{ path: 'login', component: LoginComponent },
	{ path: 'logout', component: LogoutComponent },
	{ path: 'map', component: MapComponent },
	{ path: 'user/:id', component: UserComponent },
	{ path: '**', component: NotfoundComponent }
];

@NgModule({
	imports: [RouterModule.forRoot(routes)],
	exports: [RouterModule]
})
export class AppRoutingModule { }
