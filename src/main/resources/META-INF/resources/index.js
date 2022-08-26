

function startActivity() 
{
    console.log("event handler fired.");
    let partyId = document.getElementById("partyIdStart").value;
    let req = new XMLHttpRequest();
    req.open("POST", "/notification/start/" + partyId);

    req.onload = () => {
        let outtext = document.getElementById("output");
        outtext.innerHTML = outtext.innerHTML +"\nStream started."
    }

    req.send()
}

function subscribe()
{
    console.log("Subscribe event handler fired.");
    partyId = document.getElementById("subscribeIdStart").value;
    let sse = new EventSource("/notification/subscribe/" + partyId);

    sse.addEventListener("counter", (event) => 
    {
        console.log("Message triggered counter event.");
        let outtext = document.getElementById("output");
        outtext.innerHTML = outtext.innerHTML + "\n" + event.data;
    })

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