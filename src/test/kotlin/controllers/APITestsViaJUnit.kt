package controllers

import run.qontract.core.HttpRequest
import run.qontract.core.HttpResponse
import run.qontract.mock.ContractMock
import run.qontract.mock.MockScenario
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import run.qontract.core.value.BooleanValue
import run.qontract.core.value.StringValue
import run.qontract.core.value.Value
import java.io.File
import java.net.URI
import kotlin.test.assertEquals

class APITestsViaJUnit {
    private val petStoreContract: String = File("petstore.contract").readText()

    @Test
    fun `search for available dogs`() {
        ContractMock(petStoreContract, 9000).use { mock ->
            mock.start()

            val mockRequest = HttpRequest()
                    .updateMethod("GET")
                    .updatePath("/pets")
                    .updateQueryParam("type", "dog")
                    .updateQueryParam("status", "available")
                    .updateQueryParam("name", "")
            val mockResponse = HttpResponse(200, """[{"name": "Archie", "type": "dog", "status": "available", "id": 10}, {"name": "Reggie", "type": "dog", "status": "available", "id": 20}]""")
            val scenario = MockScenario(mockRequest, mockResponse)
            mock.createMockScenario(scenario)

            val response = query("http://localhost:8080/findFirstAvailablePet?type=dog")
            assertEquals(HttpStatus.OK, response.statusCode)

            val petInfo = JSONObject(response.body)

            assertValuesExpected(petInfo, 10, "ARCHIE")
        }

        Thread.sleep(5000)
    }

    private fun assertValuesExpected(pet: JSONObject, id: Int, name: String) {
        assertEquals(id, pet.getInt("id"))
        assertEquals(name, pet.getString("name"))
    }

    private fun query(url: String): ResponseEntity<String> {
        val apiClient = RestTemplate()
        return apiClient.getForEntity(URI.create(url), String::class.java)
    }

    @Test
    fun `create pet`() {
        ContractMock(petStoreContract, 9000).use { mock ->
            mock.start()
            val requestBody = """{"name": "Archie", "type": "dog", "status": "available"}"""

            val mockRequest = HttpRequest().updateMethod("PUT").updatePath("/pets").updateBody(requestBody)
            val mockResponse = HttpResponse(200, "12")
            val scenario = MockScenario(mockRequest, mockResponse)
            mock.createMockScenario(scenario)

            val apiClient = RestTemplate()
            val url = URI.create("http://localhost:8080/create-pet")
            val requestEntity = jsonRequest(url, requestBody)
            val responseEntity = apiClient.postForEntity<String>(url, requestEntity)

            assertEquals(HttpStatus.OK, responseEntity.statusCode)
            val jsonResponse = JSONObject(responseEntity.body)
            assertEquals("success", jsonResponse.getString("status"))
            assertEquals(12, jsonResponse.getInt("id"))
        }
    }

    private fun jsonRequest(url: URI, requestBody: String): RequestEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return RequestEntity(requestBody, headers, HttpMethod.PUT, url)
    }

    companion object {
        lateinit var service: ConfigurableApplicationContext
//        lateinit var mock: ContractMock

        @BeforeAll
        @JvmStatic
        fun setUp() {
            service = SpringApplication.run(Application::class.java)
//            mock = ContractMock(File("petstore.contract").readText(), 9000)
//            mock.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            service.stop()
//            mock.close()
        }
    }
}