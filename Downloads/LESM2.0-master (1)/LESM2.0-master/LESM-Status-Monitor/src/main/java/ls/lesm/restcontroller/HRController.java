package ls.lesm.restcontroller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ls.lesm.model.Designations;
import ls.lesm.model.EmployeePhoto;
import ls.lesm.model.EmployeeStatus;
import ls.lesm.model.MasterEmployeeDetails;
import ls.lesm.payload.response.AllEmpCardDetails;
import ls.lesm.payload.response.EmpCardDetail;
import ls.lesm.payload.response.EmployeeUnderHRDropDownResponse;
import ls.lesm.payload.response.PageResponse;
import ls.lesm.repository.DesignationsRepository;
import ls.lesm.repository.EmployeePhotoRepo;
import ls.lesm.repository.MasterEmployeeDetailsRepository;
import ls.lesm.service.ExpenseService;

@RestController
@RequestMapping("/api/v1/hr")
@CrossOrigin("*")
public class HRController {

	@Autowired
	private MasterEmployeeDetailsRepository masterEmployeeDetailsRepository;

	@Autowired
	private DesignationsRepository designationsRepository;

	@Autowired
	private ExpenseService expenseService;

	@Autowired
	private EmployeePhotoRepo employeePhotoRepo;

	// UMER
	@GetMapping("/update-desg-hierarchy")
	public ResponseEntity<?> updateDesgHierar(@RequestParam int desgId, @RequestParam int newSupId) {
		Optional<Designations> desg = this.designationsRepository.findById(desgId);
		this.designationsRepository.findById(newSupId).map(id -> {
			desg.get().setDesignations(id);
			return this.designationsRepository.save(id);
		});

		return new ResponseEntity<>(HttpStatus.OK);
	}

	// UMER
	@GetMapping("/Hr-dropDown")
	public List<EmployeeUnderHRDropDownResponse> getDropDown(Principal principal) {
		String loggedInUsername = principal.getName();
		MasterEmployeeDetails loggedInEmp = this.masterEmployeeDetailsRepository.findByLancesoft(loggedInUsername);

		List<EmployeeUnderHRDropDownResponse> dropDown = this.masterEmployeeDetailsRepository
				.getEmpUnderHrDropDown(loggedInEmp.getEmpId());

		return dropDown;
	}

	// UMER
	@GetMapping("/update-supervisor-id/{empId}/{newSupId}") // this is update operation
	public ResponseEntity<?> updateSupervisiorId(@PathVariable String empId, @PathVariable int newSupId) {

		MasterEmployeeDetails employee = this.masterEmployeeDetailsRepository.findByLancesoft(empId);
		this.masterEmployeeDetailsRepository.findById(newSupId).map(id -> {
			employee.setSupervisor(id);
			return this.masterEmployeeDetailsRepository.save(id);
		});
		return new ResponseEntity<>(HttpStatus.OK);
	}

	// UMER
	// HR
	@GetMapping("/card-detail")
	public ResponseEntity<Map<String, Object>> getAllEmpCardDetails(
			@RequestParam(value = "pageNumber", defaultValue = "0", required = false) Integer pageNumber,
			@RequestParam(value = "pageSize", defaultValue = "10", required = false) Integer pageSize) {
		List<EmployeeStatus> statusList=new ArrayList<>();
		statusList.add(EmployeeStatus.ACTIVE);
		statusList.add(EmployeeStatus.BENCH);
		statusList.add(EmployeeStatus.MANAGMENT);

		Page<MasterEmployeeDetails> pageEmployees = this.masterEmployeeDetailsRepository
				.findAllByStatusIn(PageRequest.of(pageNumber, pageSize),statusList);
		
			
		//Long noOfRecords=this.masterEmployeeDetailsRepository.countNoOfRecord();
		
		ArrayList<PageResponse> list = new ArrayList<PageResponse>();
		Map<String, Object> response = new HashMap<>();

		for (MasterEmployeeDetails m : pageEmployees) {
			PageResponse emp = new PageResponse();
          if(m.getStatus()==EmployeeStatus.ACTIVE 
        		  || m.getStatus()==EmployeeStatus.BENCH 
        		  || m.getStatus()==EmployeeStatus.MANAGMENT) {//filtering exit employees
        	  
			emp.setEmployeeName(
					StringUtils.capitalize(m.getFirstName()) + " " + StringUtils.capitalize(m.getLastName()));

			emp.setEmpId(m.getEmpId());
			try {
				emp.setDesignation(m.getDesignations().getDesgNames());
			} catch (NullPointerException npe) {
				// npe.printStackTrace();
			}
			try {
				emp.setManagerName(StringUtils.capitalize(m.getSupervisor().getFirstName()) + " "
						+ StringUtils.capitalize(m.getSupervisor().getLastName()));
			} catch (NullPointerException npe) {
				// npe.printStackTrace();
			}
			try {
				emp.setLancesoftId(m.getLancesoft());
			} catch (NullPointerException npe) {
				// npe.printStackTrace();
			}
			try {
				emp.setPhoto(m.getEmployeePhoto().getProfilePic());
			} catch (NullPointerException npe) {
				// npe.printStackTrace();
			}
			list.add(emp);
          }

		}

		Page<PageResponse> pageEmps = new PageImpl<>(list);

		response.put("Employees", pageEmps.getContent());
		response.put("currentPage", pageEmployees.getNumber());
		response.put("totalItems",pageEmployees.getTotalElements() );
		response.put("totalPage", pageEmployees.getTotalPages());

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// UMER
	// HR
	@GetMapping("/card-detail-by-desg")
	public ResponseEntity<Map<String, Object>> getSortedEmpCardDetails(
			@RequestParam(value = "pageNumber", defaultValue = "0", required = false) Integer pageNumber,
			@RequestParam(value = "pageSize", defaultValue = "10", required = false) Integer pageSize,
			@RequestParam Integer desgId) {

		Page<AllEmpCardDetails> pageEmps = this.masterEmployeeDetailsRepository.getSortedEmpCardDetailsByDesg(desgId,
				PageRequest.of(pageNumber, pageSize));
		Map<String, Object> response = new HashMap<>();
		List<AllEmpCardDetails> allEmps = pageEmps.getContent();
		response.put("Employees", allEmps);
		response.put("currentPage", pageEmps.getNumber());
		response.put("totalItems", pageEmps.getTotalElements());
		response.put("totalPage", pageEmps.getTotalPages());

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// UMER
	@GetMapping("/search")
	public ResponseEntity<List<EmpCardDetail>> empSearch(@RequestParam String keyword) {
		List<AllEmpCardDetails> searchedRecord = this.masterEmployeeDetailsRepository.searchByIDNameDesg(keyword);
		
		List<EmpCardDetail> list=new ArrayList<>();
		for(AllEmpCardDetails a: searchedRecord) {
			
			if(a.getStatus().equals("ACTIVE")||a.getStatus().equals("BENCH") || a.getStatus().equals("MANAGMENT")) {
				EmpCardDetail card=new EmpCardDetail();
				card.setDesignation(a.getDesignation());
				card.setEmpId(a.getEmpId());
				card.setEmployeeName(a.getEmployeeName());
				card.setLancesoftId(a.getLancesoftId());
				card.setManagerName(a.getManagerName());
				card.setPhoto(a.getPhoto());
				list.add(card);
			}
		}
		return new ResponseEntity<List<EmpCardDetail>>(list, HttpStatus.ACCEPTED);
	}

	
	
	// UMER
	@PostMapping("/upload-photo")//s3
	public ResponseEntity<Map<String, Object>> uploadPhoto(@RequestParam String lancesoftId,
			@RequestParam MultipartFile file) {
		String publicUrl = this.expenseService.uploadFile(file);
		EmployeePhoto photo = new EmployeePhoto();
		MasterEmployeeDetails employee = this.masterEmployeeDetailsRepository.findByLancesoft(lancesoftId);

		this.masterEmployeeDetailsRepository.findById(employee.getEmpId()).map(id -> {
			photo.setMasterEmployeeDetails(id);
			return id;
		});

		photo.setProfilePic(publicUrl);
		EmployeePhoto saved = this.employeePhotoRepo.save(photo);
		this.employeePhotoRepo.findById(saved.getDocId()).map(docId -> {
			employee.setEmployeePhoto(docId);
			return this.masterEmployeeDetailsRepository.save(employee);
		});

		return new ResponseEntity<Map<String, Object>>(HttpStatus.OK);
	}
}