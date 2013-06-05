var uuid = require('node-uuid');

function main(fn) {
    fn();
}

void main(function () {
    console.log(uuid.v4().toUpperCase());
});
