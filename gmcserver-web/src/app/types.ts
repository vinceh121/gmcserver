
export interface Record {
  date: Date;
  cpm: number;
  acpm: number;
  usv: number;
}

export interface MapDevice {
  id: string;
  location?: number[];
}

export interface Device extends MapDevice {
  name?: string;
  owner: string;
  own: boolean;
  gmcId?: number;
  model?: string;
  timeline?: Record[];
}

export interface Intent {
  name: string;
  extras: any;
}
