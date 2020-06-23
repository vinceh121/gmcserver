
export interface Record {
  date: Date;
  cpm: number;
  acpm: number;
  usv: number;
}

export interface Device {
  id: string;
  name?: string;
  owner: string;
  own: boolean;
  gmcId?: number;
  model?: string;
  coord?: number[];
  timeline?: Record[];
}
