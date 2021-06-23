function toHex(byteArray){
    let hex = [];
    for (let i in byteArray){
        hex[i] = byteArray[i].toString(16).toUpperCase();
        if (hex[i].length === 1){
            hex[i] = '0' + hex[i];
        }
    }
    return hex.join('');
}

function toAscii(byteArray){
    let ascii = [];
    for (let i in byteArray){
        ascii[i] = String.fromCharCode(byteArray[i]);
    }
    return ascii.join('');
}

function base64ToBytes(base64){
    let byteKey = atob(base64);
    let byteKeyLength = byteKey.length;
    let array = new Uint8Array(new ArrayBuffer(byteKeyLength));
    for(let i = 0; i < byteKeyLength; i++) {
      array[i] = byteKey.charCodeAt(i);
    }
    return array;
}

export { toHex, base64ToBytes, toAscii};
