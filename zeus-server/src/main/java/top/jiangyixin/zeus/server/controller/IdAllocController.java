package top.jiangyixin.zeus.server.controller;


import org.springframework.web.bind.annotation.*;
import top.jiangyixin.zeus.core.common.Result;
import top.jiangyixin.zeus.core.segment.model.IdAlloc;
import top.jiangyixin.zeus.server.service.IdAllocService;
import top.jiangyixin.zeus.server.service.SegmentIdGeneratorService;

import javax.annotation.Resource;

/**
 * @author jiangyixin
 */
@RestController()
@RequestMapping("/id")
public class IdAllocController {

	@Resource
	private IdAllocService idAllocService;
	@Resource
	private SegmentIdGeneratorService segmentIdGeneratorService;

	@PostMapping("/addBizType")
	public Result addBizType(@RequestBody IdAlloc idAlloc) {
		return new Result(null, true);
	}

	@PostMapping("/inactive/{bizType}")
	public Result status(@PathVariable("bizType") String bizType) {
		return new Result(null, true);
	}

	@GetMapping("/segment/{bizType}")
	public Result<Long> getId(@PathVariable("bizType") String bizType) {
		return segmentIdGeneratorService.getId(bizType);
	}
}
