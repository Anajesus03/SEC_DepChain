// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract Blacklist is Ownable {
    constructor() Ownable(msg.sender) {}  // Pass deployer as initial owner

    mapping(address => bool) private blacklisted;


    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    function addToBlacklist(address account) external onlyOwner returns (bool) {
        blacklisted[account] = true;
        emit AddedToBlacklist(account);
        return true; // successfully added
    }

    function removeFromBlacklist(address account) external onlyOwner returns (bool) {
        blacklisted[account] = false;
        emit RemovedFromBlacklist(account);
        return true; // successfully removed
    }

    function isBlacklisted(address account) public view returns (bool) {
        return blacklisted[account];
    }

    function isBlacklistedString(address account) public view returns (string memory) {
        if(isBlacklisted(account)){
            return "It is BlackListed";
        }
        else{
             return "it is not BlackListed";
        }
    }
}


