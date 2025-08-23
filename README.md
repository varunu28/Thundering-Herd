# Thundering-Herd [![codecov](https://codecov.io/github/varunu28/Thundering-Herd/graph/badge.svg?token=AZIAO39QG5)](https://codecov.io/github/varunu28/Thundering-Herd)
Spring boot project to demonstrate a thundering herd problem &amp; its solution

## Checkpoints
 - [Commit with thundering herd problem](https://github.
   com/varunu28/Thundering-Herd/tree/f11a07db2dfa62245430b52badbf3128866bb3c5) :x:
 - [Commit with thundering herd problem fixed](https://github.
   com/varunu28/Thundering-Herd/tree/08c199bad9e13c0d86a4707435f5d152aeb3b01f) :white_check_mark:

## How to run?
- Start the Spring Boot application 
- Run the following Go program (`go run main.go <product_id>`) to send concurrent requests for the GET endpoint to 
  trigger thundering herd

```go
package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"sync"
)

func main() {
	args := os.Args
	if len(args) != 2 {
		fmt.Println("Usage: go run main.go <product_id>")
		return
	}

	numRequests := 5
	var wg sync.WaitGroup

	productId := os.Args[1]
	url := fmt.Sprintf("http://localhost:8080/api/v1/products/%s", productId)

	// Channel to signal all goroutines to start simultaneously
	startSignal := make(chan struct{})

	// Channel to collect results
	results := make(chan string, numRequests)

	for i := 0; i < numRequests; i++ {
		wg.Add(1)
		go func(requestID int) {
			defer wg.Done()

			<-startSignal // Wait for the signal to start

			resp, err := http.Get(url)
			if err != nil {
				fmt.Printf("Error: %v\n", err)
			}
			defer resp.Body.Close()
			body, err := io.ReadAll(resp.Body)
			if err != nil {
				fmt.Printf("Error reading body: %v\n", err)
			}
			result := fmt.Sprintf("Request %d - Status: %d, Response: %s",
				requestID, resp.StatusCode, string(body))
			results <- result
		}(i)
	}

	fmt.Println("Starting all requests...")
	close(startSignal) // Signal all goroutines to start

	wg.Wait()
	close(results)
	for result := range results {
		fmt.Println(result)
	}
}
```