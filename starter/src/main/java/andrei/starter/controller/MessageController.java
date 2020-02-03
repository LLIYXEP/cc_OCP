package andrei.starter.controller;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import andrei.starter.domain.Message;
import andrei.starter.domain.User;
import andrei.starter.repos.MessageRepo;
import andrei.starter.repos.UserRepo;
import andrei.starter.service.MessageService;

@Controller
public class MessageController {
	@Autowired
	private UserRepo userRepo;
	
    @Autowired
    private MessageRepo messageRepo;
    
    @Autowired
    private MessageService messageService;
    
    //Proverka Upload Directory
    @Value("${upload.path}")
    private String uploadPath;

    @GetMapping("/")
    public String greeting(Map<String, Object> model) {
        return "greeting";
    }

    @GetMapping("/main")
    public String main(@RequestParam(required = false, defaultValue = "") String filter, 
    		Model model, 
    		@PageableDefault(sort = { "id" }, direction = Direction.DESC) Pageable pageable,
    		@AuthenticationPrincipal  User user) {
        
    	Page<Message> page = messageService.messageList(pageable, filter);

        model.addAttribute("page", page);
        model.addAttribute("url", "/main");
        model.addAttribute("filter", filter);

        return "main";
    }

    @PostMapping("/main")
    public String add(
    		@AuthenticationPrincipal  User user,
    		@Valid Message message,
    		BindingResult bindingResult,
    		Model model,
    		@RequestParam ("file") MultipartFile file
    		) throws IllegalStateException, IOException {
    	
        message.setAuthor(user);
        
        if (bindingResult.hasErrors()) {
            Map<String, String> errorsMap = ControllerUtils.getErrors(bindingResult);

            model.mergeAttributes(errorsMap);
            model.addAttribute("message", message);
        } else {
        
        saveFile(message, file);
        
        model.addAttribute("message", null);

        messageRepo.save(message);
		}

        Iterable<Message> messages = messageRepo.findAll();

        model.addAttribute("messages", messages);

        return "main";
    }

	private void saveFile(Message message, MultipartFile file) throws IOException {
		if (file != null && !file.getOriginalFilename().isEmpty()) {
        	File uploadDir = new File(uploadPath);
        	if (!uploadDir.exists()) {
				uploadDir.mkdir();
			}
        	
        	String uuidFile = UUID.randomUUID().toString();
        	String resultFieName = uuidFile + "." + file.getOriginalFilename();
        	
        	file.transferTo(new File(uploadPath + "/" + resultFieName));
        	
			message.setFileName(resultFieName);
		}
	}
    
    @GetMapping("/user-messages/{author}")
    public String userMessages(@AuthenticationPrincipal  User currentUser, 
    		@PathVariable User author,
    		Model model,
    		@RequestParam(required = false) Message message,
    		@PageableDefault(sort = { "id" }, direction = Direction.DESC) Pageable pageable) {
    	
    	Page<Message> page = messageService.messageListForUser(pageable, currentUser, author);
    	
    	
    	
    	model.addAttribute("userChanel", author);
    	model.addAttribute("subscriptionsCount", author.getSubscribtions().size());
    	model.addAttribute("subscribersCount", author.getSubscribers().size());
    	model.addAttribute("isSubscriber", author.getSubscribers().contains(currentUser));
    	model.addAttribute("page", page);
    	model.addAttribute("message", message);
    	model.addAttribute("isCurrentUser", currentUser.equals(author));
    	model.addAttribute("url", "/user-messages/" + author.getId());
    	
    	return "userMessages";
    }
    
    @PostMapping("/user-messages/{user}")
    public String updateMessages(@AuthenticationPrincipal  User currentUser, 
    		@PathVariable Long user,
    		@RequestParam("id") Message message,
    		@RequestParam("text") String text,
    		@RequestParam("tag") String tag,
    		@RequestParam ("file") MultipartFile file) throws IOException {
    	
    	if (message.getAuthor().equals(currentUser)) {
			if (!StringUtils.isEmpty(text)) {
				message.setText(text);
			}
			
			if (!StringUtils.isEmpty(tag)) {
				message.setTag(tag);
			}
		}
    	saveFile(message, file);
    	messageRepo.save(message);
    		
    	return "redirect:/user-messages/" + user;
    }
    
    
}
