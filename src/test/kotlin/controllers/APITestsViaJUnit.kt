package controllers

import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.http.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import `in`.specmatic.stub.ContractStub
import `in`.specmatic.stub.createStub
import java.net.URI

class APITestsViaJUnit {
    @Test
    fun `search for available dogs`() {
        val apiClient = RestTemplate()
        val response = apiClient.getForEntity(URI.create("http://localhost:8080/findFirstAvailablePet?type=dog"), String::class.java)

        assertEquals(HttpStatus.OK, response.statusCode)
        val petInfo = JSONObject(response.body)
        assertEquals(10, petInfo.getInt("id"))
        assertEquals("ARCHIE", petInfo.getString("name"))
    }

    @Test
    fun `create pet`() {
        val requestBody = """{"name": "Archie", "type": "dog", "status": "available"}"""

        val apiClient = RestTemplate()
        val url = URI.create("http://localhost:8080/create-pet")
        val requestEntity = authenticatedJsonRequest(url, requestBody)
        val responseEntity = apiClient.postForEntity<String>(url, requestEntity)

        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val jsonResponse = JSONObject(responseEntity.body)
        assertEquals("success", jsonResponse.getString("status"))
        assertEquals(12, jsonResponse.getInt("id"))
    }

    private fun authenticatedJsonRequest(url: URI, requestBody: String): RequestEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return RequestEntity(requestBody, headers, HttpMethod.PUT, url)
    }

    companion object {
        private var service: ConfigurableApplicationContext? = null

        private lateinit var stub: ContractStub

        @BeforeAll
        @JvmStatic
        fun setUp() {
            stub = createStub()
            service = SpringApplication.run(Application::class.java)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            service?.stop()
            stub.close()
        }
    }
}
