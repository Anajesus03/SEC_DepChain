// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract Blacklist {
    address public owner;
    mapping(address => bool) private blacklist;

    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    modifier onlyOwner() {
        require(msg.sender == owner, "Not authorized");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    function addToBlacklist(address _account) external onlyOwner returns (bool) {
        blacklist[_account] = true;
        emit AddedToBlacklist(_account);
        return true;
    }

    function removeFromBlacklist(address _account) external onlyOwner returns (bool) {
        blacklist[_account] = false;
        emit RemovedFromBlacklist(_account);
        return true;
    }

    function isBlacklisted(address _account) external view returns (bool) {
        return blacklist[_account];
    }
}
