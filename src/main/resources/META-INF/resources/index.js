

function startActivity() 
{
    console.log("event handler fired.");
    let req = new XMLHttpRequest();
    req.open("POST", "/notification/start/0");

    req.onload = () => {
        let outtext = document.getElementById("output");
        outtext.innerHTML = outtext.innerHTML +"\nStream started."
    }

    req.send()
}

function subscribe()
{
    console.log("Subscribe event handler fired.");
    let sse = new EventSource("/notification/subscribe/0");

    sse.addEventListener("counter", (event) => {console.log("Message triggered counter event.");})

    sse.onmessage = (event) => {
        let outtext = document.getElementById("output");
        outtext.innerHTML = outtext.innerHTML + "\n" + event.data;
    }
    sse.onerror = (err) => {
        let outtext = document.getElementById("output");
        outtext.innerHTML = outtext.innerHTML + "\n" + err;
    }
}

console.log("script loaded.");