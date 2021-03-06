package com.kazuki43zoo.app.error;

import com.kazuki43zoo.core.message.Message;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@RequestMapping("error")
@Controller
public class ErrorController {

    @RequestMapping("invalidSession")
    public String handleSessionError(final RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(Message.FW_VALID_SESSION_NOT_EXISTS.resultMessages());
        return "redirect:/";
    }

}
