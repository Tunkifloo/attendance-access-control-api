package com.iot.attendance.application.service;

import com.iot.attendance.application.dto.request.UpdateSystemConfigRequest;
import com.iot.attendance.application.dto.response.SystemConfigurationResponse;

public interface SystemConfigurationService {

    SystemConfigurationResponse getCurrentConfiguration();

    SystemConfigurationResponse updateConfiguration(UpdateSystemConfigRequest request);

    SystemConfigurationResponse initializeDefaultConfiguration();

    SystemConfigurationResponse enableSimulationMode(UpdateSystemConfigRequest request);

    SystemConfigurationResponse disableSimulationMode();
}
