// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

contract Blacklist is Ownable {
    constructor() Ownable(msg.sender) {}  // Pass deployer as initial owner

    mapping(address => bool) private blacklisted;


    event AddedToBlacklist(address indexed account);
    event RemovedFromBlacklist(address indexed account);

    function addToBlacklist(address account) external onlyOwner returns (string memory) {
        if (blacklisted[account] ) {return "Account already blacklisted, confirmed by me the supreme owner";}
        blacklisted[account] = true;
        emit AddedToBlacklist(account);
        return "Added new entry in blackList";
    }

    function removeFromBlacklist(address account) external onlyOwner returns (string memory) {
        if (!blacklisted[account] ) {return "Account is not blacklisted, confirmed by me the supreme owner";}
        blacklisted[account] = false;
        emit RemovedFromBlacklist(account);
        return "Removed from blackList";
    }

    function isBlacklisted(address account) public view returns (bool) {
        return blacklisted[account];
    }

    function isBlacklisted_String(address account) public view returns (string memory) {
        if(blacklisted[account]){return "True, it is blacklisted";}
        return "False, it is not blacklisted";
    }
    
    function sayHelloWorld() public pure returns (string memory){
        return "hello";
    }
}


