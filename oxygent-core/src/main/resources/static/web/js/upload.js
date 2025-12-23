/**
 * File Upload Utilities
 * @author OxyGent Team
 * @version 1.0.0
 * @since 1.0.0
 */

function uploadFile(file, callback) {
    console.log(file);
    const formData = new FormData();
    formData.append('file', file);

    fetch('../upload', {
        method: 'POST',
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            callback(data)
            // document.getElementById('result').textContent = 'Upload successful: ' + JSON.stringify(data);
        })
        .catch(error => {
            console.log(error);
            // document.getElementById('result').textContent = 'Upload failed: ' + error;
        });
}