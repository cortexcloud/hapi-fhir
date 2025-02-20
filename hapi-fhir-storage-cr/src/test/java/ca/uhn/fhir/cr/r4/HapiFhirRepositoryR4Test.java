package ca.uhn.fhir.cr.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.*;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ca.uhn.fhir.cr.repo.HapiFhirRepository;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.api.Constants;

public class HapiFhirRepositoryR4Test extends BaseCrR4TestServer {

	@Autowired
	RestfulServer myRestfulServer;
	private static final String MY_TEST_DATA =
		"ca/uhn/fhir/cr/r4/immunization/Patients_Encounters_Immunizations_Practitioners.json";

	@Test
	void crudTest() {
		var requestDetails = setupRequestDetails();
		//register repo
		//regster providers
		var repository = new HapiFhirRepository(myDaoRegistry, requestDetails, myRestfulServer);
		var result = repository
			.create(new Patient().addName(new HumanName().setFamily("Test").addGiven("Name1")));
		assertEquals(true, result.getCreated());
		var patient = (Patient) result.getResource();
		assertEquals(1, patient.getName().size());
		assertEquals("Test", patient.getName().get(0).getFamily());
		assertEquals(1, patient.getName().get(0).getGiven().size());
		patient.getName().get(0).addGiven("Name2");
		repository.update(patient);
		var updatedPatient = repository.read(Patient.class, patient.getIdElement());
		assertEquals(2, updatedPatient.getName().get(0).getGiven().size());
		repository.delete(Patient.class, patient.getIdElement());
		var ex = assertThrows(Exception.class,
			() -> repository.read(Patient.class, new IdType(patient.getIdElement().getIdPart())));
		assertTrue(ex.getMessage().contains("Resource was deleted"));
	}

	@Test
	void canSearchMoreThan50Patients() {
		loadBundle(MY_TEST_DATA);
		var expectedPatientCount = 63;

		ourPagingProvider.setMaximumPageSize(100);
		var repository = new HapiFhirRepository(myDaoRegistry, setupRequestDetails(), myRestfulServer);
		// get all patient resources posted
		var result = repository.search(Bundle.class, Patient.class, withCountParam(100));
		assertEquals(expectedPatientCount, result.getTotal());
		// count all resources in result
		int counter = 0;
		for (var e : result.getEntry()) {
			counter++;
		}
		// verify all patient resources captured
		assertEquals(expectedPatientCount, counter,
			"Patient search results don't match available resources");
	}

	@Test
	void canSearchWithPagination() {
		loadBundle(MY_TEST_DATA);

		var requestDetails = setupRequestDetails();
		var repository = new HapiFhirRepository(myDaoRegistry, requestDetails, myRestfulServer);
		var result = repository.search(Bundle.class, Patient.class, withCountParam(20));
		assertEquals(20, result.getEntry().size());
		var next = result.getLink().get(1);
		assertEquals("next", next.getRelation());
		var nextUrl = next.getUrl();
		var nextResult = repository.link(Bundle.class, nextUrl);
		assertEquals(20, nextResult.getEntry().size());
		assertEquals(false,
			result.getEntry().stream().map(e -> e.getResource().getIdPart()).anyMatch(
				i -> nextResult.getEntry().stream().map(e -> e.getResource().getIdPart())
					.collect(Collectors.toList()).contains(i)));
	}

	@Test
	void transactionReadsPatientResources() {
		var expectedPatientCount = 63;
		var theBundle = readResource(Bundle.class, MY_TEST_DATA);
		ourPagingProvider.setMaximumPageSize(100);
		var repository = new HapiFhirRepository(myDaoRegistry, setupRequestDetails(), myRestfulServer);
		repository.transaction(theBundle);
		var result = repository.search(Bundle.class, Patient.class, withCountParam(100));
		// count all resources in result
		int counter = 0;
		for (Object i : result.getEntry()) {
			counter++;
		}
		// verify all patient resources captured
		assertEquals(expectedPatientCount, counter,
			"Patient search results don't match available resources");
	}

	@Test
	void transactionReadsEncounterResources() {
		var expectedEncounterCount = 652;
		var theBundle = readResource(Bundle.class, MY_TEST_DATA);
		ourPagingProvider.setMaximumPageSize(1000);
		var repository = new HapiFhirRepository(myDaoRegistry, setupRequestDetails(), myRestfulServer);
		repository.transaction(theBundle);
		var result = repository.search(Bundle.class, Encounter.class, withCountParam(1000));
		// count all resources in result
		int counter = 0;
		for (Object i : result.getEntry()) {
			counter++;
		}
		// verify all encounter resources captured
		assertEquals(expectedEncounterCount, counter,
			"Encounter search results don't match available resources");
	}

	@Test
	void repositorySearchForEncounterWithMatchingCode() {
		loadBundle("ColorectalCancerScreeningsFHIR-bundle.json");

		//SearchConverter validation test for repository
		List<IQueryParameterType> codeList = new ArrayList<>();
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://snomed.info/sct").setCode("185463005")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://snomed.info/sct").setCode("185464004")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://snomed.info/sct").setCode("185465003")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://snomed.info/sct").setCode("30346009")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://snomed.info/sct").setCode("3391000175108")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://snomed.info/sct").setCode("37894004")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://snomed.info/sct").setCode("439740005")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99201")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99202")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99203")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99204")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99205")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99212")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99213")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99214")));
		codeList.add(new TokenParam(new InternalCodingDt().setSystem("http://www.ama-assn.org/go/cpt").setCode("99215")));

		// replicate repository searchParam list
		Map<String, List<IQueryParameterType>> searchParams = Map.of("type", codeList, "subject", Collections.singletonList(new ReferenceParam("Patient/numer-EXM130")));

		var repository = new HapiFhirRepository(myDaoRegistry, setupRequestDetails(), myRestfulServer);

		// replicate search for valueset codes
		var result = repository.search(Bundle.class, Encounter.class, searchParams);

		// count all resources in result
		int counter = 0;
		for (Object i : result.getEntry()) {
			counter++;
		}
		// verify patient encounter was the only one found
		assertEquals(1, counter,
				"Encounter search results don't match available resources");
	}

	@Test
	void transactionReadsImmunizationResources() {
		var expectedEncounterCount = 638;
		var theBundle = readResource(Bundle.class, MY_TEST_DATA);
		ourPagingProvider.setMaximumPageSize(1000);
		var repository = new HapiFhirRepository(myDaoRegistry, setupRequestDetails(), myRestfulServer);
		repository.transaction(theBundle);
		var result = repository.search(Bundle.class, Immunization.class, withCountParam(1000));
		// count all resources in result
		int counter = 0;
		for (Object i : result.getEntry()) {
			counter++;
		}
		// verify all immunization resources captured
		assertEquals(expectedEncounterCount, counter,
			"Immunization search results don't match available resources");
	}

	Map<String, List<IQueryParameterType>> withEmptySearchParams() {
		return new HashMap<>();
	}

	Map<String, List<IQueryParameterType>> withCountParam(int theCount) {
		var params = withEmptySearchParams();
		params.put(Constants.PARAM_COUNT, Collections.singletonList(new NumberParam(theCount)));
		return params;
	}
}
