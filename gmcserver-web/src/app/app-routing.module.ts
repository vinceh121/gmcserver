import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { DeviceComponent } from './device/device.component';
import { NotfoundComponent } from './notfound/notfound.component';
import { LoginComponent } from './login/login.component';
import { MapComponent } from './map/map.component';


const routes: Routes = [
	{ path: '', component: HomeComponent },
	{ path: 'device/:id', component: DeviceComponent },
	{ path: 'login', component: LoginComponent },
	{ path: 'map', component: MapComponent },
	{ path: '**', component: NotfoundComponent }
];

@NgModule({
	imports: [RouterModule.forRoot(routes)],
	exports: [RouterModule]
})
export class AppRoutingModule { }
