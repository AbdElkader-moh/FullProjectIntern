import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SharedDashboard } from './shared-dashboard';

describe('SharedDashboard', () => {
  let component: SharedDashboard;
  let fixture: ComponentFixture<SharedDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedDashboard],
    }).compileComponents();

    fixture = TestBed.createComponent(SharedDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
