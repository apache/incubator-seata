package org.apache.seata.namingserver.controller;

import org.apache.seata.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	@GetMapping("/health")
	public Result<?> healthCheck() {
		return new Result<>();
	}


}
