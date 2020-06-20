
export interface Record {
  cpm: number;
  acpm: number;
  usv: number;
  date: Date;
}

export interface Device {
  id: string;
  name: string;
  owner: boolean;
  gmcId?: number;
  model?: string;
  coord?: number[];
  timeline?: Record[];
}
