package top.jiangyixin.zeus.server.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import top.jiangyixin.zeus.core.common.Result;
import top.jiangyixin.zeus.core.segment.model.IdAlloc;

/**
 * @author jiangyixin
 */
@RestController("/id")
public class IdAllocController {

	@PostMapping("/addBizType")
	public Result addBizType(@RequestBody IdAlloc idAlloc) {
	}

	@PostMapping("/status")
	public Result status(@RequestBody)
}
